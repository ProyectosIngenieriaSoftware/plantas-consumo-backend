package sv.edu.ues.fmp.flora.util;

/**
 * Utilidades de texto compartidas por todos los modulos de la API.
 * <p>
 * Nace de un hallazgo de QA: en varios modulos se colaban duplicados que solo
 * diferian en espacios al inicio, al final o entre palabras ("Hoja" frente a
 * "Hoja "). Cualquier servicio que valide unicidad sobre un texto deberia
 * normalizarlo con {@link #normalizar(String)} antes de consultar.
 * <p>
 * Regla de uso: se normaliza ANTES de consultar duplicados, y ese mismo valor
 * normalizado es el que se consulta y el que se persiste. Si la validacion
 * mirara el valor crudo y el INSERT el normalizado, verian cosas distintas y
 * el duplicado llegaria a la base como un 409 generico.
 */
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
