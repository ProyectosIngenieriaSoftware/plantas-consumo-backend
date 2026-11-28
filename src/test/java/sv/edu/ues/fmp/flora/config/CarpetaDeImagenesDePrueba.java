package sv.edu.ues.fmp.flora.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * La carpeta donde escriben las pruebas de imagenes propias.
 *
 * <h2>Por que no la carpeta real</h2>
 * {@code app.imagenes.directorio} apunta por defecto a una carpeta del usuario
 * que en una maquina de desarrollo tiene las imagenes de verdad. Una prueba que
 * escribiera ahi dejaria archivos sueltos que nadie borra --residuo-- y, peor,
 * los contaria como suyos: una prueba que compruebe "la carpeta quedo vacia"
 * fallaria por las imagenes legitimas que ya estaban.
 *
 * <h2>Por que una carpeta compartida y no un @TempDir por clase</h2>
 * Dos razones.
 * <p>
 * La primera es el orden de inicializacion: un {@code static @TempDir} lo
 * rellena la extension de JUnit y {@code @DynamicPropertySource} lo lee al
 * crear el contexto de Spring, y entre esas dos extensiones el orden no esta
 * garantizado de forma util. Creando el directorio en el inicializador estatico
 * de esta clase, el valor existe antes de que nadie pregunte.
 * <p>
 * La segunda es el cache de contextos: Spring reutiliza el contexto entre
 * clases de prueba solo si la clave coincide, y las propiedades dinamicas
 * forman parte de la clave. Con un directorio distinto por clase, cada clase
 * levantaria su propio contexto; con este, todas las pruebas de imagenes
 * comparten uno. Son unos cuantos segundos en cada ejecucion de la suite.
 *
 * <h2>Quien la borra</h2>
 * {@link #limpiar()}, desde el {@code @AfterEach} de cada clase: vacia el
 * contenido y deja la carpeta. Asi una prueba nunca ve los archivos de otra, y
 * la comprobacion de "no se escribio nada" significa lo que dice. La carpeta en
 * si vive en el temporal del sistema, que el sistema operativo recicla.
 */
public final class CarpetaDeImagenesDePrueba {

    /** Se crea una sola vez, antes de que ningun contexto de Spring pregunte por ella. */
    private static final Path CARPETA;

    static {
        try {
            CARPETA = Files.createTempDirectory("flora-imagenes-prueba");
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "No se pudo crear la carpeta temporal de las pruebas de imagenes", e);
        }
    }

    private CarpetaDeImagenesDePrueba() {
    }

    /**
     * Apunta {@code app.imagenes.directorio} a la carpeta de pruebas.
     * <p>
     * Cada clase de prueba de imagenes la llama desde su propio
     * {@code @DynamicPropertySource}. El valor es el mismo en todas a
     * proposito.
     */
    public static void registrar(DynamicPropertyRegistry registro) {
        registro.add("app.imagenes.directorio", CARPETA::toString);
    }

    public static Path carpeta() {
        return CARPETA;
    }

    /** Los archivos que hay ahora mismo. */
    public static long archivos() {
        try (Stream<Path> contenido = Files.list(CARPETA)) {
            return contenido.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo listar " + CARPETA, e);
        }
    }

    public static boolean existe(String nombre) {
        return nombre != null && Files.isRegularFile(CARPETA.resolve(nombre));
    }

    /** Vacia la carpeta sin borrarla; idempotente. */
    public static void limpiar() {
        if (!Files.isDirectory(CARPETA)) {
            return;
        }

        try (Stream<Path> contenido = Files.walk(CARPETA)) {
            contenido.sorted(Comparator.reverseOrder())
                    .filter(ruta -> !ruta.equals(CARPETA))
                    .forEach(ruta -> {
                        try {
                            Files.deleteIfExists(ruta);
                        } catch (IOException e) {
                            throw new UncheckedIOException("No se pudo borrar " + ruta, e);
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo limpiar " + CARPETA, e);
        }
    }
}
