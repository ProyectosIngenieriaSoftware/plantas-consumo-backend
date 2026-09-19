package sv.edu.ues.fmp.flora.exception;

/**
 * Se lanza desde la capa de servicio cuando se intenta una transicion de estado
 * que el flujo editorial no permite (por ejemplo publicar una especie que nadie
 * ha validado, o rechazar una que sigue en BORRADOR).
 * El {@link GlobalExceptionHandler} la traduce a HTTP 409.
 */
public class EstadoInvalidoException extends RuntimeException {

    public EstadoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
