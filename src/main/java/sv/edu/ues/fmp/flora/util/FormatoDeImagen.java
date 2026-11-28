package sv.edu.ues.fmp.flora.util;

import java.util.Locale;

/**
 * Los tres formatos de imagen que el catalogo acepta subir, con la firma
 * binaria que identifica a cada uno.
 *
 * <h2>Por que se miran los bytes y no lo que dice el cliente</h2>
 * El {@code Content-Type} de una parte multipart y la extension del nombre de
 * archivo los escribe quien sube: las dos cosas se cambian sin tocar el
 * contenido. Un {@code informe.pdf} renombrado a {@code foto.jpg} llega con
 * extension {@code .jpg} y, si el cliente quiere, con
 * {@code Content-Type: image/jpeg}; nada de eso lo convierte en una imagen.
 * Guardarlo dejaria en la carpeta un archivo que el navegador no puede mostrar
 * --y que se serviria con un Content-Type que no le corresponde-- ademas de
 * ser la via por la que se cuela cualquier otra cosa en el servidor.
 * <p>
 * Los primeros bytes de un archivo, en cambio, los escribe el programa que lo
 * genero. No es una garantia criptografica --nadie impide poner la cabecera de
 * un PNG delante de otra cosa-- pero cierra el caso real, que es el archivo
 * equivocado o renombrado, y es lo que se puede comprobar sin anadir ninguna
 * dependencia.
 *
 * <h2>Las firmas</h2>
 * <ul>
 *   <li><strong>JPEG</strong>: {@code FF D8 FF}. Los dos primeros bytes son el
 *       marcador SOI y el tercero abre el marcador siguiente, que varia
 *       ({@code E0} en JFIF, {@code E1} en Exif...), por eso no se comprueba un
 *       cuarto byte.</li>
 *   <li><strong>PNG</strong>: {@code 89 50 4E 47 0D 0A 1A 0A}, los ocho bytes
 *       de su firma. El {@code 89} inicial y el par {@code 0D 0A} estan ahi
 *       justamente para que un archivo PNG corrompido por una transferencia en
 *       modo texto deje de validar.</li>
 *   <li><strong>WebP</strong>: contenedor RIFF, asi que la firma esta partida:
 *       {@code "RIFF"} en los bytes 0-3, cuatro bytes de tamano que se ignoran,
 *       y {@code "WEBP"} en los bytes 8-11. Comprobar solo {@code "RIFF"}
 *       aceptaria tambien un WAV o un AVI.</li>
 * </ul>
 *
 * <h2>Lo que esta clase no hace</h2>
 * No decodifica la imagen ni comprueba que el resto del archivo sea coherente:
 * un JPEG truncado despues de la cabecera pasa. Hacerlo de verdad pedia
 * decodificar, y eso es trabajo de una libreria; aqui el criterio es el que
 * cubre el caso que se reporto sin anadir dependencias al {@code pom.xml}.
 */
public enum FormatoDeImagen {

    JPEG("image/jpeg", ".jpg", new String[] { "jpg", "jpeg" },
            new int[] { 0xFF, 0xD8, 0xFF }, 0),

    PNG("image/png", ".png", new String[] { "png" },
            new int[] { 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A }, 0),

    /** La segunda mitad de la firma, {@code "WEBP"}, va en el byte 8. */
    WEBP("image/webp", ".webp", new String[] { "webp" },
            new int[] { 0x52, 0x49, 0x46, 0x46 }, 0,
            new int[] { 0x57, 0x45, 0x42, 0x50 }, 8);

    /** Lo que se devuelve como {@code Content-Type} al servir el archivo. */
    private final String tipoContenido;

    /** La extension con la que se guarda, normalizada: un JPEG siempre {@code .jpg}. */
    private final String extension;

    /** Las extensiones que el cliente puede traer para este formato, en minuscula. */
    private final String[] extensionesAceptadas;

    private final int[] firma;
    private final int firmaDesde;

    /** Segundo tramo de la firma, para los contenedores que la llevan partida. */
    private final int[] segundaFirma;
    private final int segundaFirmaDesde;

    FormatoDeImagen(String tipoContenido, String extension, String[] extensionesAceptadas,
            int[] firma, int firmaDesde) {
        this(tipoContenido, extension, extensionesAceptadas, firma, firmaDesde, null, 0);
    }

    FormatoDeImagen(String tipoContenido, String extension, String[] extensionesAceptadas,
            int[] firma, int firmaDesde, int[] segundaFirma, int segundaFirmaDesde) {
        this.tipoContenido = tipoContenido;
        this.extension = extension;
        this.extensionesAceptadas = extensionesAceptadas;
        this.firma = firma;
        this.firmaDesde = firmaDesde;
        this.segundaFirma = segundaFirma;
        this.segundaFirmaDesde = segundaFirmaDesde;
    }

    public String tipoContenido() {
        return tipoContenido;
    }

    public String extension() {
        return extension;
    }

    /**
     * El formato cuya firma llevan estos bytes, o {@code null} si no es ninguno
     * de los tres.
     * <p>
     * Un {@code null} aqui significa "esto no es una imagen de las que
     * aceptamos", y quien llama lo traduce al 400. No se lanza desde aqui para
     * que la clase siga siendo una utilidad sin opinion sobre codigos HTTP.
     */
    public static FormatoDeImagen detectar(byte[] contenido) {
        if (contenido == null) {
            return null;
        }

        for (FormatoDeImagen formato : values()) {
            if (formato.coincide(contenido)) {
                return formato;
            }
        }
        return null;
    }

    /**
     * El formato que corresponde a un {@code Content-Type} declarado, o
     * {@code null} si no se reconoce.
     * <p>
     * Un {@code null} no es un error: los navegadores mandan
     * {@code application/octet-stream} cuando no saben, y rechazar por eso
     * seria rechazar imagenes legitimas. Quien llama lo trata como "el cliente
     * no afirma nada" y deja decidir a los bytes. Lo que si es un error es que
     * el cliente afirme un formato y los bytes digan otro.
     * <p>
     * Se ignora lo que venga despues de {@code ;} --un
     * {@code image/jpeg; charset=binary}-- y las mayusculas.
     */
    public static FormatoDeImagen porTipoContenido(String declarado) {
        if (declarado == null || declarado.isBlank()) {
            return null;
        }

        String limpio = declarado.split(";")[0].strip().toLowerCase(Locale.ROOT);

        for (FormatoDeImagen formato : values()) {
            if (formato.tipoContenido.equals(limpio)) {
                return formato;
            }
        }
        return null;
    }

    /**
     * El formato que corresponde a la extension de un nombre de archivo, o
     * {@code null} si no se reconoce o si el nombre no trae extension.
     * <p>
     * Igual que con el tipo declarado, el {@code null} es "no afirma nada". El
     * nombre no se usa para nada mas: el archivo se guarda con un nombre
     * generado, nunca con este.
     */
    public static FormatoDeImagen porNombreDeArchivo(String nombre) {
        if (nombre == null) {
            return null;
        }

        int punto = nombre.lastIndexOf('.');
        if (punto < 0 || punto == nombre.length() - 1) {
            return null;
        }

        String extension = nombre.substring(punto + 1).strip().toLowerCase(Locale.ROOT);

        for (FormatoDeImagen formato : values()) {
            for (String aceptada : formato.extensionesAceptadas) {
                if (aceptada.equals(extension)) {
                    return formato;
                }
            }
        }
        return null;
    }

    /** Los tres tipos admitidos, para nombrarlos en los mensajes de error. */
    public static String tiposAdmitidos() {
        StringBuilder admitidos = new StringBuilder();
        for (FormatoDeImagen formato : values()) {
            if (!admitidos.isEmpty()) {
                admitidos.append(", ");
            }
            admitidos.append(formato.tipoContenido);
        }
        return admitidos.toString();
    }

    private boolean coincide(byte[] contenido) {
        return llevaFirma(contenido, firma, firmaDesde)
                && (segundaFirma == null || llevaFirma(contenido, segundaFirma, segundaFirmaDesde));
    }

    /**
     * Compara byte a byte enmascarando con {@code 0xFF}: en Java un
     * {@code byte} tiene signo, asi que el {@code 0x89} de la firma del PNG se
     * lee como -119 y la comparacion directa contra el {@code int} 0x89 nunca
     * seria cierta.
     */
    private static boolean llevaFirma(byte[] contenido, int[] firma, int desde) {
        if (contenido.length < desde + firma.length) {
            return false;
        }

        for (int i = 0; i < firma.length; i++) {
            if ((contenido[desde + i] & 0xFF) != firma[i]) {
                return false;
            }
        }
        return true;
    }
}
