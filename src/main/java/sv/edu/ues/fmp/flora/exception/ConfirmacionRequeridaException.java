package sv.edu.ues.fmp.flora.exception;

/**
 * Se lanza cuando la operacion es valida pero tiene una consecuencia que quien
 * llama tiene que aceptar antes de que se aplique. No se cambia nada: la
 * peticion se rechaza entera y hay que reenviarla confirmando.
 *
 * <h2>Por que es una excepcion propia y no {@link EstadoInvalidoException}</h2>
 * Las dos salen con 409, pero significan cosas distintas para quien llama y el
 * cliente tiene que poder separarlas sin leer el texto del mensaje:
 * <ul>
 *   <li>{@link EstadoInvalidoException} es "esto no se puede hacer". Reintentar
 *       igual no sirve; hay que cambiar el estado de la ficha primero.</li>
 *   <li>Esta es "esto se puede hacer, pero confirma". El mismo envio con la
 *       confirmacion puesta funciona.</li>
 * </ul>
 * El {@link GlobalExceptionHandler} la traduce a HTTP 409 con
 * {@code error: "Confirmación requerida"}, distinto del
 * {@code "Transición de estado no válida"} de la otra, que es el campo por el
 * que el front las distingue.
 *
 * <h2>Quien la usa hoy</h2>
 * Las dos ediciones de contenido propio de la especie --{@code PUT /{id}} y
 * {@code PATCH /{id}/taxonomia}--, cuando la ficha esta PUBLICADA: editarla la
 * saca del catalogo publico y la devuelve a revision, y eso no debe pasar en
 * silencio.
 */
public class ConfirmacionRequeridaException extends RuntimeException {

    public ConfirmacionRequeridaException(String mensaje) {
        super(mensaje);
    }
}
