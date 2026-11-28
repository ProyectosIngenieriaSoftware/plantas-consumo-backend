package sv.edu.ues.fmp.flora.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import sv.edu.ues.fmp.flora.exception.ParametroInvalidoException;
import sv.edu.ues.fmp.flora.exception.RecursoNoEncontradoException;
import sv.edu.ues.fmp.flora.util.FormatoDeImagen;

/**
 * La carpeta del servidor donde viven los binarios de las imagenes propias, y
 * las unicas operaciones que se hacen sobre ella: validar lo que llega,
 * escribirlo con un nombre generado y volverlo a leer.
 *
 * <h2>Por que el archivo no va en la base</h2>
 * Decision tomada: el binario al sistema de archivos y en la base solo la
 * referencia. Una imagen de hasta 20MB en una columna {@code bytea} la arrastra
 * cada consulta que no la excluya a mano, engorda los respaldos de una base que
 * hoy son texto, y obliga a pasar el archivo entero por el driver para
 * servirlo.
 *
 * <h2>La carpeta</h2>
 * Sale de {@code app.imagenes.directorio}, con default fuera del proyecto y
 * override por {@code FLORA_IMAGENES_DIR} --mismo patron que
 * {@code JWT_SECRET}--. Se resuelve una sola vez al arrancar, a ruta absoluta y
 * normalizada, y se crea si no existe. Si no se puede crear, la aplicacion no
 * arranca: un servidor que acepta subidas y no tiene donde ponerlas es peor que
 * uno que no arranca, porque falla una peticion a la vez y sin que nadie mire.
 * <p>
 * Todo se maneja con {@link Path} y {@link Path#resolve}, nunca concatenando
 * {@code "/"}: el equipo trabaja en Windows y la ruta tiene que resolverse al
 * separador del sistema en los dos sitios.
 *
 * <h2>El nombre lo pone el sistema</h2>
 * Un {@link UUID} mas la extension que corresponde al formato <em>detectado</em>.
 * El nombre del cliente no se usa para nada:
 * <ul>
 *   <li><strong>Colisiones.</strong> Dos personas suben {@code foto.jpg} y la
 *       segunda sobreescribe a la primera, que sigue teniendo una fila
 *       apuntando a un archivo que ya no es el suyo.</li>
 *   <li><strong>Path traversal.</strong> Un nombre como
 *       {@code ../../src/main/resources/application.properties} sale de la
 *       carpeta. El nombre del cliente llega ademas por una cabecera que
 *       cualquiera edita.</li>
 * </ul>
 * El original se guarda en su propia columna, solo para mostrarlo.
 *
 * <h2>Orden: primero la fila, despues el archivo</h2>
 * Quien llama guarda la fila y despues pide escribir. Al reves, un INSERT que
 * fallara --el CHECK de la fuente unica, el indice de la principal activa--
 * dejaria en la carpeta un archivo que ninguna fila reclama. En este orden, si
 * la escritura falla se lanza y la transaccion se lleva la fila. Queda una
 * ventana estrecha --que el commit falle despues de escribir-- cuyo resultado
 * es un archivo huerfano: ocupa espacio y nada mas, porque nada lo referencia y
 * ningun nombre se repite.
 */
@Component
public class AlmacenamientoDeImagenes {

    private static final Logger log = LoggerFactory.getLogger(AlmacenamientoDeImagenes.class);

    private final String directorioConfigurado;

    /**
     * El mismo limite que aplica el parser de multipart, leido de la misma
     * propiedad.
     * <p>
     * No es un numero nuevo: se inyecta
     * {@code spring.servlet.multipart.max-file-size} para que no haya dos
     * fuentes de verdad que puedan discrepar. En la practica el parser rechaza
     * antes de que esta clase se ejecute --de ahi el 413 que ve el cliente--;
     * esta comprobacion cubre el caso de que alguien suba el limite del parser
     * sin querer, o llame al servicio sin pasar por HTTP.
     */
    private final DataSize tamanioMaximo;

    /** Resuelto en {@link #prepararCarpeta()}; absoluto y normalizado. */
    private Path directorio;

    public AlmacenamientoDeImagenes(
            @Value("${app.imagenes.directorio}") String directorioConfigurado,
            @Value("${spring.servlet.multipart.max-file-size}") DataSize tamanioMaximo) {
        this.directorioConfigurado = directorioConfigurado;
        this.tamanioMaximo = tamanioMaximo;
    }

    /**
     * Resuelve la carpeta y la crea si hace falta, al arrancar.
     * <p>
     * {@code createDirectories} es idempotente: con la carpeta ya creada no
     * hace nada y no falla, asi que no hay que preguntar antes.
     */
    @PostConstruct
    void prepararCarpeta() {
        directorio = Paths.get(directorioConfigurado).toAbsolutePath().normalize();

        try {
            Files.createDirectories(directorio);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo preparar la carpeta de imágenes: " + directorio, e);
        }

        log.info("Imágenes propias de especie en {}", directorio);
    }

    /** La carpeta en uso, para los mensajes de diagnostico y las pruebas. */
    public Path directorio() {
        return directorio;
    }

    /**
     * Comprueba que lo que llego sea una de las tres imagenes admitidas y
     * devuelve su formato. No escribe nada.
     * <p>
     * Se llama <strong>antes</strong> de tocar la base y antes de tocar el
     * disco, que es lo que hace que un archivo rechazado no deje rastro en
     * ninguno de los dos.
     *
     * <h2>Las tres comprobaciones</h2>
     * <ol>
     *   <li><strong>Que haya algo.</strong> Una parte vacia no es una imagen de
     *       cero bytes, es un formulario mal armado.</li>
     *   <li><strong>El tamano.</strong> Ver {@link #tamanioMaximo}.</li>
     *   <li><strong>La firma binaria.</strong> {@link FormatoDeImagen#detectar}
     *       sobre los bytes reales. Si no es ninguno de los tres formatos, 400:
     *       aqui se corta el {@code .txt} o el {@code .pdf} renombrado
     *       a {@code .jpg}.</li>
     *   <li><strong>Que el cliente no se contradiga.</strong> Si el
     *       {@code Content-Type} declarado o la extension nombran un formato
     *       <em>reconocido y distinto</em> del que dicen los bytes, tambien es
     *       400. Un PNG llamado {@code foto.jpg} no se guarda en silencio: o el
     *       archivo o el nombre esta equivocado, y adivinar cual deja al
     *       catalogo con metadatos que no describen el archivo.</li>
     * </ol>
     * Un tipo declarado que no se reconoce --el {@code application/octet-stream}
     * que manda un navegador cuando no sabe-- o un nombre sin extension no son
     * contradicciones: no afirman nada y deciden los bytes. Rechazarlos seria
     * rechazar subidas legitimas.
     *
     * @return el formato detectado, que es el que manda para la extension con
     *         la que se guarda y para el {@code Content-Type} con el que se
     *         sirve
     * @throws ParametroInvalidoException      400: vacio, no es imagen, o el
     *                                         cliente se contradice
     * @throws MaxUploadSizeExceededException  413: excede el limite
     */
    public FormatoDeImagen validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ParametroInvalidoException("El archivo de la imagen viene vacío");
        }

        if (archivo.getSize() > tamanioMaximo.toBytes()) {
            throw new MaxUploadSizeExceededException(tamanioMaximo.toBytes());
        }

        byte[] contenido = leerBytes(archivo);

        FormatoDeImagen detectado = FormatoDeImagen.detectar(contenido);
        if (detectado == null) {
            throw new ParametroInvalidoException(
                    "El archivo no es una imagen válida: su contenido no corresponde a ninguno "
                            + "de los formatos admitidos (" + FormatoDeImagen.tiposAdmitidos()
                            + "). La extensión y el tipo declarado no bastan; se comprueba el "
                            + "contenido real del archivo.");
        }

        exigirQueCoincida(detectado,
                FormatoDeImagen.porTipoContenido(archivo.getContentType()),
                "el tipo declarado (" + archivo.getContentType() + ")");

        exigirQueCoincida(detectado,
                FormatoDeImagen.porNombreDeArchivo(archivo.getOriginalFilename()),
                "la extensión del nombre (" + archivo.getOriginalFilename() + ")");

        return detectado;
    }

    /**
     * El nombre con el que se va a guardar: UUID mas la extension del formato
     * detectado.
     * <p>
     * Se genera aparte de {@link #escribir} porque la fila se guarda primero y
     * necesita el nombre antes de que exista el archivo.
     */
    public String generarNombre(FormatoDeImagen formato) {
        return UUID.randomUUID() + formato.extension();
    }

    /**
     * Escribe el archivo en la carpeta con el nombre dado.
     * <p>
     * {@code CREATE_NEW} falla si ya existe en vez de sobreescribir: con un
     * UUID no deberia pasar nunca, y si pasara, sobreescribir borraria la
     * imagen de otra ficha sin que nadie se enterara. Que falle es la
     * respuesta correcta.
     *
     * @throws UncheckedIOException si no se puede escribir; quien llama esta en
     *                              una transaccion, asi que la fila se va con
     *                              la excepcion
     */
    public void escribir(String nombre, MultipartFile archivo) {
        Path destino = resolver(nombre);

        try {
            Files.write(destino, leerBytes(archivo),
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "No se pudo guardar la imagen en " + destino, e);
        }
    }

    /**
     * Devuelve el contenido del archivo de una imagen propia.
     *
     * @throws RecursoNoEncontradoException si el archivo no esta en la carpeta.
     *                                      Puede pasar con una base restaurada
     *                                      sin su carpeta, o si alguien borro
     *                                      el archivo a mano: la baja logica
     *                                      nunca lo borra. Es un 404 y no un
     *                                      500 porque, para quien pide, el
     *                                      binario no esta.
     */
    public byte[] leer(String nombre) {
        Path origen = resolver(nombre);

        if (!Files.isRegularFile(origen)) {
            log.warn("La imagen {} tiene fila pero no archivo en {}", nombre, directorio);
            throw new RecursoNoEncontradoException(
                    "El archivo de la imagen no está disponible");
        }

        try {
            return Files.readAllBytes(origen);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la imagen " + origen, e);
        }
    }

    /** Si el archivo de una imagen propia sigue en la carpeta. */
    public boolean existeArchivo(String nombre) {
        return nombre != null && Files.isRegularFile(resolver(nombre));
    }

    /**
     * El {@link Path} de un nombre guardado, comprobando que no se salga de la
     * carpeta.
     *
     * <h2>Por que se vuelve a comprobar</h2>
     * El nombre lo genera {@link #generarNombre} y es un UUID, asi que no
     * deberia poder contener {@code ..} ni separadores. Pero el valor llega de
     * una columna de la base, y entre el UUID y esta lectura hay una fila que
     * se puede editar por SQL, restaurar de un respaldo de otra epoca o
     * modificar desde una ruta futura que no exista hoy. La comprobacion vale
     * dos lineas y convierte "lee cualquier archivo del servidor" en un 404.
     */
    private Path resolver(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new RecursoNoEncontradoException("El archivo de la imagen no está disponible");
        }

        Path destino = directorio.resolve(nombre).normalize();

        if (!destino.startsWith(directorio)) {
            log.warn("Nombre de archivo fuera de la carpeta de imágenes: {}", nombre);
            throw new RecursoNoEncontradoException("El archivo de la imagen no está disponible");
        }

        return destino;
    }

    /**
     * Rechaza que el cliente afirme un formato distinto del que dicen los
     * bytes. Un {@code null} en {@code afirmado} es "no afirma nada" y pasa.
     */
    private static void exigirQueCoincida(FormatoDeImagen detectado, FormatoDeImagen afirmado,
            String queAfirma) {
        if (afirmado != null && afirmado != detectado) {
            throw new ParametroInvalidoException(
                    "El contenido del archivo es " + detectado.tipoContenido()
                            + ", pero " + queAfirma + " no corresponde a ese formato");
        }
    }

    /**
     * Los bytes de la parte multipart. Se leen en memoria porque hay que verlos
     * para validarlos y el limite son 20MB, que es un tamano que cabe sin
     * discusion; un stream no ayudaria aqui porque la firma esta al principio
     * pero el tamano total ya lo dio el parser.
     */
    private static byte[] leerBytes(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo enviado", e);
        }
    }
}
