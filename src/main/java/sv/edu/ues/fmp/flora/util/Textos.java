package sv.edu.ues.fmp.flora.util;


public final class Textos {

    private Textos() {
    }

    /**
     * Quita espacios al inicio y al final y colapsa espacios internos a uno.
     * Devuelve null si el resultado queda vacio.
     */
    public static String normalizar(String valor) {
        if (valor == null) return null;
        String limpio = valor.strip().replaceAll("\s+", " ");
        return limpio.isEmpty() ? null : limpio;
    }
}
