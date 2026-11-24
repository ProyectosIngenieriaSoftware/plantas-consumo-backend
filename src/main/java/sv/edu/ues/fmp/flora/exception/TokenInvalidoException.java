package sv.edu.ues.fmp.flora.exception;

/**
 * El token de recuperacion de clave no existe, ya fue usado o ya expiro.
 * El {@link GlobalExceptionHandler} la traduce a HTTP 400.
 */
public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException(String mensaje) {
        super(mensaje);
    }
}
