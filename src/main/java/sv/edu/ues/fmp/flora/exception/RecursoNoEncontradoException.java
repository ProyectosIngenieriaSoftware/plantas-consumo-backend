package sv.edu.ues.fmp.flora.exception;

/**
 * Se lanza desde la capa de servicio cuando el registro solicitado no existe.
 * El {@link GlobalExceptionHandler} la traduce a HTTP 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
