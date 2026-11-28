package sv.edu.ues.fmp.flora.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.dto.request.ImagenEspecieArchivoRequest;
import sv.edu.ues.fmp.flora.dto.request.ImagenEspecieRequest;
import sv.edu.ues.fmp.flora.dto.response.ImagenEspecieResponse;
import sv.edu.ues.fmp.flora.entity.*;
import sv.edu.ues.fmp.flora.entity.enums.OrigenImagen;
import sv.edu.ues.fmp.flora.exception.EstadoInvalidoException;
import sv.edu.ues.fmp.flora.exception.RecursoNoEncontradoException;
import sv.edu.ues.fmp.flora.mapper.ImagenEspecieMapper;
import sv.edu.ues.fmp.flora.repository.EspecieRepository;
import sv.edu.ues.fmp.flora.repository.ImagenEspecieRepository;
import sv.edu.ues.fmp.flora.service.AlmacenamientoDeImagenes;
import sv.edu.ues.fmp.flora.service.ArchivoDeImagen;
import sv.edu.ues.fmp.flora.service.ImagenEspecieService;
import sv.edu.ues.fmp.flora.service.VisibilidadDeEspecie;
import sv.edu.ues.fmp.flora.util.FormatoDeImagen;

/** Gestiona imágenes, valida sus relaciones y coordina el índice parcial de principal activa. */
@Service
@RequiredArgsConstructor
@Transactional
public class ImagenEspecieServiceImpl implements ImagenEspecieService {
    private final ImagenEspecieRepository imagenEspecieRepository;
    private final EspecieRepository especieRepository;
    private final ValidadorParteEspecie validadorParteEspecie;
    private final ImagenEspecieMapper imagenEspecieMapper;

    /** La carpeta del servidor y las validaciones del archivo que llega. */
    private final AlmacenamientoDeImagenes almacenamiento;

    /**
     * Decide si el contenido de la especie padre se puede mostrar a quien
     * llama. Lo que no se puede ver se responde como lo que no existe.
     */
    private final VisibilidadDeEspecie visibilidadDeEspecie;

    /** Distingue especie inexistente de un listado vacío; permite consultar el historial. */
    @Override
    @Transactional(readOnly = true)
    public List<ImagenEspecieResponse> listarPorEspecie(Long idEspecie, boolean soloActivas) {
        if (!especieRepository.existsById(idEspecie)) {
            throw new RecursoNoEncontradoException("No existe la especie con id " + idEspecie);
        }
        List<ImagenEspecie> imagenes = soloActivas
                ? imagenEspecieRepository.findByEspecieIdEspecieAndActivaTrueOrderByIdImagenAsc(idEspecie)
                : imagenEspecieRepository.findByEspecieIdEspecieOrderByIdImagenAsc(idEspecie);
        return imagenes.stream().map(imagenEspecieMapper::toResponse).toList();
    }

    /** Consulta una imagen sin filtrar su estado. */
    @Override
    @Transactional(readOnly = true)
    public ImagenEspecieResponse obtenerPorId(Long id) {
        return imagenEspecieMapper.toResponse(buscarVisibleOFallar(id));
    }

    /**
     * La imagen, pero solo si la especie a la que pertenece se puede ver.
     * <p>
     * Cuando no, lanza el <strong>mismo</strong> 404 que {@link #buscarOFallar}
     * da para un id que no existe: los dos salen de {@link #noEncontrada} para
     * que sean identicos y la imagen de un borrador no se pueda distinguir de
     * una que no existe. Un 403 aqui confirmaria que ese id esta ocupado.
     */
    private ImagenEspecie buscarVisibleOFallar(Long id) {
        ImagenEspecie entidad = buscarOFallar(id);

        if (!visibilidadDeEspecie.puedeVer(entidad.getEspecie().getIdEspecie())) {
            throw noEncontrada(id);
        }

        return entidad;
    }

    /** Valida especie y parte antes de desplazar a la principal e insertar. */
    @Override
    public ImagenEspecieResponse crear(Long idEspecie, ImagenEspecieRequest request) {
        Especie especie = especieRepository.findById(idEspecie)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la especie con id " + idEspecie));
        verificarEspecieActiva(especie);
        EspecieParteComestible parte = validadorParteEspecie.resolver(idEspecie, request.getIdEspecieParte());
        if (Boolean.TRUE.equals(request.getEsPrincipal())) {
            desmarcarPrincipalActual(idEspecie);
        }
        ImagenEspecie nueva = imagenEspecieMapper.toEntity(request, especie, parte);
        return imagenEspecieMapper.toResponse(imagenEspecieRepository.save(nueva));
    }

    /**
     * Sube el archivo de una imagen propia.
     *
     * <h2>El orden importa, y es este</h2>
     * <ol>
     *   <li><strong>Validar el archivo primero que todo.</strong> Antes de
     *       mirar la especie: un {@code .pdf} renombrado se rechaza igual
     *       apunte a la ficha que apunte, y validarlo primero hace que un
     *       archivo malo no cueste ni una consulta.</li>
     *   <li><strong>Resolver especie y parte.</strong> Las mismas
     *       comprobaciones que {@link #crear}, con los mismos codigos: 404 si
     *       la especie no existe, 409 si esta desactivada, y la parte tiene que
     *       ser suya.</li>
     *   <li><strong>Desplazar la principal</strong> si esta llega como
     *       principal, igual que al registrar un enlace.</li>
     *   <li><strong>Guardar la fila y hacer flush.</strong></li>
     *   <li><strong>Escribir el archivo al final.</strong></li>
     * </ol>
     *
     * <h2>Por que la fila antes que el archivo</h2>
     * Al reves, un INSERT que fallara --{@code ck_imagen_fuente_unica},
     * {@code uk_imagen_principal_especie}, la FK de la parte-- dejaria en la
     * carpeta un archivo que ninguna fila reclama, y el cliente recibiria un
     * error habiendo escrito en el disco del servidor. En este orden, si la
     * escritura falla se lanza dentro de la transaccion y el rollback se lleva
     * la fila: no queda ni una cosa ni la otra.
     * <p>
     * El {@code flush()} es lo que hace que ese orden signifique algo: sin el,
     * Hibernate retrasaria el INSERT hasta el commit, o sea hasta despues de
     * escribir el archivo, y los CHECK de la base se comprobarian cuando el
     * binario ya estuviera en disco.
     *
     * <h2>El refresco de la fecha de la especie</h2>
     * No se hace aqui. El INSERT de esta fila dispara
     * {@code trg_imagen_especie_toca_especie} (V23), que actualiza la especie
     * dueña para que su propio BEFORE UPDATE le ponga la fecha. Por eso este
     * metodo no menciona {@code fecha_actualizacion} ni toca el modulo de
     * especie, y por eso el refresco funciona igual desde cualquier ruta que
     * cambie imagenes, incluida una que se escriba manana.
     */
    @Override
    public ImagenEspecieResponse subirArchivo(Long idEspecie, ImagenEspecieArchivoRequest request,
            MultipartFile archivo) {

        // Primero el archivo: lo que no es una imagen admitida no llega ni a
        // consultar la especie, y sobre todo no llega al disco.
        FormatoDeImagen formato = almacenamiento.validar(archivo);

        Especie especie = especieRepository.findById(idEspecie)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la especie con id " + idEspecie));
        verificarEspecieActiva(especie);
        EspecieParteComestible parte = validadorParteEspecie.resolver(idEspecie,
                request.getIdEspecieParte());

        if (Boolean.TRUE.equals(request.getEsPrincipal())) {
            desmarcarPrincipalActual(idEspecie);
        }

        String nombreArchivo = almacenamiento.generarNombre(formato);

        ImagenEspecie nueva = imagenEspecieMapper.toEntityArchivo(request, especie, parte,
                nombreArchivo, formato, archivo.getSize(), archivo.getOriginalFilename());

        ImagenEspecie guardada = imagenEspecieRepository.save(nueva);

        // Adelanta el INSERT: los CHECK de la base se comprueban ahora, con el
        // disco todavia sin tocar.
        imagenEspecieRepository.flush();

        almacenamiento.escribir(nombreArchivo, archivo);

        return imagenEspecieMapper.toResponse(guardada);
    }

    /**
     * El binario de una imagen propia, con la visibilidad de su especie.
     *
     * <h2>La visibilidad es la misma de siempre</h2>
     * Se resuelve con {@link #buscarVisibleOFallar}, el mismo metodo que usa
     * {@link #obtenerPorId}: si la especie no se puede ver, sale el 404 de "no
     * existe la imagen con id X", identico al de un id inventado. Asi el
     * archivo de una ficha en BORRADOR no se puede distinguir de uno que no
     * existe, que es justo el oraculo que un 403 abriria. Una imagen de una
     * ficha PUBLICADA la sirve cualquiera sin token, porque la ruta esta en
     * {@code ESPECIE_PUBLICA}/{@code CATALOGO_PUBLICO} para GET y el guard la
     * deja pasar.
     *
     * <h2>Una imagen URL no tiene archivo</h2>
     * No hay nada que servir: el binario lo tiene otro servidor y lo que
     * guardamos es la direccion, que ya sale en el Response. Es un 409 y no un
     * 404 a proposito --la imagen existe y se puede ver, lo que no encaja es la
     * operacion-- y el mensaje dice donde esta la URL.
     *
     * <h2>Y si la fila esta pero el archivo no</h2>
     * Lo resuelve {@link AlmacenamientoDeImagenes#leer} con un 404: una base
     * restaurada sin su carpeta, o un archivo borrado a mano. La baja logica
     * nunca borra archivos, asi que por las rutas de la API no se llega a ese
     * estado.
     *
     * <h2>Se sirve aunque este inactiva</h2>
     * Mismo criterio que {@link #obtenerPorId}, que no filtra por {@code activa}:
     * la baja logica es para el catalogo, y quien ya puede ver la ficha puede
     * consultar su historial de imagenes. Lo que decide si se ve es la especie,
     * no el estado de la imagen.
     */
    @Override
    @Transactional(readOnly = true)
    public ArchivoDeImagen leerArchivo(Long id) {
        ImagenEspecie entidad = buscarVisibleOFallar(id);

        if (entidad.getOrigen() != OrigenImagen.PROPIA) {
            throw new EstadoInvalidoException(
                    "La imagen " + id + " es un enlace externo y no tiene archivo en el "
                            + "servidor; su dirección está en el campo urlImagen.");
        }

        return new ArchivoDeImagen(
                almacenamiento.leer(entidad.getRutaArchivo()),
                entidad.getTipoContenido(),
                entidad.getNombreOriginal());
    }

    /**
     * Solo desplaza a la principal en la transición false a true.
     *
     * <h2>Por que rechaza una imagen propia</h2>
     * Este PUT reemplaza los campos editables, y uno de ellos es la URL, que
     * {@code ImagenEspecieRequest} exige. Sobre una fila PROPIA eso escribiria
     * una URL en una imagen cuyo origen dice que el binario esta en disco:
     * {@code ck_imagen_fuente_unica} la rechazaria y el cliente recibiria un
     * 409 de integridad --sin mensaje util, por como se enmascara
     * {@code DataIntegrityViolationException}-- en vez de saber que pidio algo
     * que no existe.
     * <p>
     * Se corta antes, con un 409 que lo dice. Las operaciones que si valen
     * sobre una imagen propia --darla de baja, reactivarla, marcarla como
     * principal-- no pasan por aqui y funcionan igual que en un enlace.
     * <p>
     * Queda pendiente, y es una ausencia consciente de este cambio: no hay
     * forma de editar el titulo o el texto alternativo de una imagen propia.
     * Pedia su propia entrada --un PUT sin URL-- y no estaba en el alcance.
     */
    @Override
    public ImagenEspecieResponse actualizar(Long id, ImagenEspecieRequest request) {
        ImagenEspecie entidad = buscarOFallar(id);

        if (entidad.getOrigen() == OrigenImagen.PROPIA) {
            throw new EstadoInvalidoException(
                    "La imagen " + id + " es un archivo propio y no se edita por esta ruta, "
                            + "que reemplaza la URL. Para una imagen propia se puede dar de "
                            + "baja, reactivar o marcar como principal.");
        }

        verificarEspecieActiva(entidad.getEspecie());
        Long idEspecie = entidad.getEspecie().getIdEspecie();
        EspecieParteComestible parte = validadorParteEspecie.resolver(idEspecie, request.getIdEspecieParte());
        boolean promocion = Boolean.TRUE.equals(request.getEsPrincipal())
                && !Boolean.TRUE.equals(entidad.getEsPrincipal());
        if (promocion) {
            verificarImagenActiva(entidad);
            desmarcarPrincipalActual(idEspecie);
        }
        // Entidad gestionada: Hibernate emite el UPDATE sin llamar a save().
        imagenEspecieMapper.updateEntity(entidad, request, parte);
        return imagenEspecieMapper.toResponse(entidad);
    }

    /** Baja idempotente; desactiva la imagen y desmarca su condición de principal. */
    @Override
    public void desactivar(Long id) {
        ImagenEspecie entidad = buscarOFallar(id);
        entidad.setActiva(false);
        entidad.setEsPrincipal(false);
    }

    /** Reactiva de forma idempotente como no-principal, sin desplazar a la principal activa. */
    @Override
    public ImagenEspecieResponse activar(Long id) {
        ImagenEspecie entidad = buscarOFallar(id);
        if (Boolean.TRUE.equals(entidad.getActiva())) {
            return imagenEspecieMapper.toResponse(entidad);
        }
        verificarEspecieActiva(entidad.getEspecie());
        validadorParteEspecie.resolver(entidad.getEspecie().getIdEspecie(),
                entidad.getEspecieParte() == null ? null : entidad.getEspecieParte().getIdEspecieParte());
        entidad.setEsPrincipal(false);
        entidad.setActiva(true);
        return imagenEspecieMapper.toResponse(entidad);
    }

    /** Repetir la promoción no cambia la principal ni la desmarca a sí misma. */
    @Override
    public ImagenEspecieResponse marcarComoPrincipal(Long id) {
        ImagenEspecie entidad = buscarOFallar(id);
        // El estado de la especie se comprueba antes del atajo de
        // idempotencia: con la especie inactiva el resto de operaciones ya se
        // rechazaban, pero volver a marcar como principal una imagen que ya lo
        // era respondia 200 (hallazgo IM-H01 del 3er informe de QA).
        verificarEspecieActiva(entidad.getEspecie());
        verificarImagenActiva(entidad);
        if (Boolean.TRUE.equals(entidad.getEsPrincipal())) {
            return imagenEspecieMapper.toResponse(entidad);
        }
        desmarcarPrincipalActual(entidad.getEspecie().getIdEspecie());
        entidad.setEsPrincipal(true);
        return imagenEspecieMapper.toResponse(entidad);
    }

    /** Libera el índice parcial antes del INSERT o UPDATE que promueve otra imagen. */
    private void desmarcarPrincipalActual(Long idEspecie) {
        imagenEspecieRepository.findByEspecieIdEspecieAndEsPrincipalTrueAndActivaTrue(idEspecie)
                .ifPresent(anterior -> {
                    anterior.setEsPrincipal(false);
                    // No se llama a save(): anterior es una entidad gestionada.
                    // Hibernate pone los INSERT antes de los UPDATE y tampoco garantiza
                    // el orden entre dos UPDATE. El índice parcial no es DEFERRABLE:
                    // PostgreSQL lo comprueba sentencia a sentencia. flush() adelanta
                    // la desmarcación antes de guardar o promover la nueva principal.
                    imagenEspecieRepository.flush();
                });
    }

    /** Resuelve el registro o devuelve el 404 de negocio. */
    private ImagenEspecie buscarOFallar(Long id) {
        return imagenEspecieRepository.findById(id)
                .orElseThrow(() -> noEncontrada(id));
    }

    /** El 404 del recurso, en un solo sitio para que no se separe de si mismo. */
    private static RecursoNoEncontradoException noEncontrada(Long id) {
        return new RecursoNoEncontradoException("No existe la imagen con id " + id);
    }

    /** Las nuevas asociaciones y modificaciones requieren una especie activa. */
    private void verificarEspecieActiva(Especie especie) {
        if (!Boolean.TRUE.equals(especie.getActiva())) {
            throw new EstadoInvalidoException("La especie '" + especie.getNombreCientifico()
                    + "' está desactivada; no se pueden registrar o promover sus imágenes.");
        }
    }

    /** Una imagen desactivada conserva su historial, pero no puede promocionarse. */
    private void verificarImagenActiva(ImagenEspecie imagen) {
        if (!Boolean.TRUE.equals(imagen.getActiva())) {
            throw new EstadoInvalidoException("Una imagen desactivada no puede marcarse como principal.");
        }
    }
}
