package sv.edu.ues.fmp.flora.exception;

/**
 * Se lanza desde la capa de servicio cuando se intenta guardar un registro que
 * violaria una restriccion de unicidad del negocio.
 * El {@link GlobalExceptionHandler} la traduce a HTTP 409.
 */
public class RecursoDuplicadoException extends RuntimeException {

    public RecursoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
