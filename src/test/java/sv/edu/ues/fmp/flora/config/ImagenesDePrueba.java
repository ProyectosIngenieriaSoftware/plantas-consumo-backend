package sv.edu.ues.fmp.flora.config;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Contenidos binarios para las pruebas de subida: los tres formatos que el
 * catalogo acepta y los que tiene que rechazar.
 *
 * <h2>Por que se construyen a mano y no se leen de un archivo de recursos</h2>
 * Lo que se esta probando es la deteccion por los primeros bytes, asi que lo
 * que hace falta es controlar <strong>exactamente</strong> esos bytes. Un
 * archivo de ejemplo en {@code src/test/resources} serviria igual, pero
 * obligaria a abrirlo con un visor para saber que firma lleva, y una prueba que
 * falle no diria si el problema esta en el codigo o en el archivo de muestra.
 * Escritos aqui, la firma esta a la vista y al lado del comentario que dice de
 * donde sale.
 * <p>
 * No son imagenes decodificables: llevan la firma correcta y relleno detras. Es
 * suficiente porque la validacion no decodifica --eso pedia una libreria y no
 * se anaden dependencias-- y es lo que el codigo promete comprobar.
 */
public final class ImagenesDePrueba {

    private ImagenesDePrueba() {
    }

    /** {@code FF D8 FF E0}: SOI mas el marcador APP0 de JFIF. */
    public static byte[] jpeg() {
        return conRelleno(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0 });
    }

    /** Los ocho bytes de la firma PNG. */
    public static byte[] png() {
        return conRelleno(new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A });
    }

    /**
     * Contenedor RIFF: {@code "RIFF"}, cuatro bytes de tamano y {@code "WEBP"}.
     * El tamano se deja en ceros porque la validacion no lo mira.
     */
    public static byte[] webp() {
        return conRelleno(new byte[] {
                0x52, 0x49, 0x46, 0x46,
                0x00, 0x00, 0x00, 0x00,
                0x57, 0x45, 0x42, 0x50 });
    }

    /** Texto plano: no lleva ninguna firma de imagen. */
    public static byte[] texto() {
        return "Esto es un archivo de texto, no una imagen.\n"
                .repeat(10)
                .getBytes(StandardCharsets.UTF_8);
    }

    /** {@code %PDF-1.7}: la firma de un PDF, que no esta entre las admitidas. */
    public static byte[] pdf() {
        byte[] firma = "%PDF-1.7\n".getBytes(StandardCharsets.UTF_8);
        return conRelleno(firma);
    }

    /**
     * Solo {@code "RIFF"} sin {@code "WEBP"} detras: la forma de un WAV o un
     * AVI. Comprobar media firma lo aceptaria.
     */
    public static byte[] riffQueNoEsWebp() {
        return conRelleno(new byte[] {
                0x52, 0x49, 0x46, 0x46,
                0x00, 0x00, 0x00, 0x00,
                0x57, 0x41, 0x56, 0x45 });
    }

    /** Un contenido de {@code megas} MB, para el limite de tamano. */
    public static byte[] jpegDeMegas(int megas) {
        byte[] grande = new byte[megas * 1024 * 1024];
        grande[0] = (byte) 0xFF;
        grande[1] = (byte) 0xD8;
        grande[2] = (byte) 0xFF;
        grande[3] = (byte) 0xE0;
        return grande;
    }

    /** La firma mas relleno, para que el archivo no sean solo cuatro bytes. */
    private static byte[] conRelleno(byte[] firma) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        salida.writeBytes(firma);
        salida.writeBytes(new byte[256]);
        return salida.toByteArray();
    }
}
