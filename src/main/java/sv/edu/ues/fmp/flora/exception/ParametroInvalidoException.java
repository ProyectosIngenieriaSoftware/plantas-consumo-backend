package sv.edu.ues.fmp.flora.exception;

/**
 * Un parametro de la peticion (por ejemplo, de paginacion) trae un valor
 * fuera de rango -> 400 BAD REQUEST.
 */
public class ParametroInvalidoException extends RuntimeException {
    public ParametroInvalidoException(String message) {
        super(message);
    }
}
