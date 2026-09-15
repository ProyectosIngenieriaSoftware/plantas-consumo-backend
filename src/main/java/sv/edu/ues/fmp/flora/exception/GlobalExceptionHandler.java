package sv.edu.ues.fmp.flora.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Captura en un solo lugar las excepciones de toda la API y las convierte en
 * respuestas HTTP con un cuerpo {@link ErrorResponse} uniforme.
 * Gracias a esto los controladores quedan libres de bloques try/catch.
 * <p>
 * El manejador de {@link DataIntegrityViolationException} es una <em>red de
 * seguridad</em>, no el mecanismo previsto: cubre de golpe las restricciones de
 * las 28 tablas del esquema, pero su mensaje es necesariamente generico. Si un
 * cliente recibe ese 409 generico, significa que a algun servicio le falta
 * anticipar su propia restriccion y devolver un mensaje especifico.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * El recurso pedido no existe -> 404 NOT FOUND.
     */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarRecursoNoEncontrado(
            RecursoNoEncontradoException ex,
            HttpServletRequest request) {

        ErrorResponse cuerpo = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .estado(HttpStatus.NOT_FOUND.value())
                .error("Recurso no encontrado")
                .mensaje(ex.getMessage())
                .ruta(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpo);
    }

    /**
     * El registro chocaria con uno existente -> 409 CONFLICT.
     */
    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> manejarRecursoDuplicado(
            RecursoDuplicadoException ex,
            HttpServletRequest request) {

        ErrorResponse cuerpo = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .estado(HttpStatus.CONFLICT.value())
                .error("Conflicto")
                .mensaje(ex.getMessage())
                .ruta(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(cuerpo);
    }

    /**
     * Ultima red: una restriccion de la base rechazo la operacion y ningun
     * servicio la anticipo -> 409 CONFLICT.
     * <p>
     * El mensaje es deliberadamente generico. El detalle de la excepcion
     * incluye el nombre de la restriccion y el SQL que fallo, y el sistema
     * tiene un area publica: esa informacion no debe salir en una respuesta
     * HTTP. Para diagnosticar queda el log del servidor.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> manejarIntegridad(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        ErrorResponse cuerpo = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .estado(HttpStatus.CONFLICT.value())
                .error("Conflicto de integridad")
                .mensaje("La operación viola una restricción de la base de datos")
                .ruta(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(cuerpo);
    }

    /**
     * La transicion de estado pedida no esta permitida por el flujo editorial
     * -> 409 CONFLICT. Es un conflicto con el estado actual del recurso, no un
     * error de formato de la peticion, por eso 409 y no 400.
     */
    @ExceptionHandler(EstadoInvalidoException.class)
    public ResponseEntity<ErrorResponse> manejarEstadoInvalido(
            EstadoInvalidoException ex,
            HttpServletRequest request) {

        ErrorResponse cuerpo = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .estado(HttpStatus.CONFLICT.value())
                .error("Transición de estado no válida")
                .mensaje(ex.getMessage())
                .ruta(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(cuerpo);
    }

    /**
     * Login fallido (usuario/correo inexistente, clave incorrecta o cuenta
     * desactivada) -> 401 UNAUTHORIZED. El mensaje es siempre el mismo para
     * no revelar cual de esas tres cosas paso.
     */
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponse> manejarCredencialesInvalidas(
            CredencialesInvalidasException ex,
            HttpServletRequest request) {

        ErrorResponse cuerpo = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .estado(HttpStatus.UNAUTHORIZED.value())
                .error("No autorizado")
                .mensaje(ex.getMessage())
                .ruta(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(cuerpo);
    }

    /**
     * El id recibido (path variable) no es un valor valido -> 400 BAD REQUEST.
     */
    @ExceptionHandler(IdInvalidoException.class)
    public ResponseEntity<ErrorResponse> manejarIdInvalido(
            IdInvalidoException ex,
            HttpServletRequest request) {

        ErrorResponse cuerpo = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .estado(HttpStatus.BAD_REQUEST.value())
                .error("Id inválido")
                .mensaje(ex.getMessage())
                .ruta(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpo);
    }

    /**
     * Un path variable o request param no se pudo convertir al tipo esperado
     * (por ejemplo, "/api/usuarios/abc") -> 400 BAD REQUEST.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoInvalido(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        ErrorResponse cuerpo = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .estado(HttpStatus.BAD_REQUEST.value())
                .error("Parámetro inválido")
                .mensaje("El valor de '" + ex.getName() + "' no tiene el formato esperado")
                .ruta(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpo);
    }

    /**
     * Falla alguna anotacion de Bean Validation en un {@code @Valid @RequestBody}
     * -> 400 BAD REQUEST con el detalle campo por campo.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarErroresDeValidacion(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        // Se recorren todos los errores de campo para que el cliente reciba de
        // una sola vez todo lo que debe corregir, y no solo el primer fallo.
        Map<String, String> erroresValidacion = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            erroresValidacion.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse cuerpo = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .estado(HttpStatus.BAD_REQUEST.value())
                .error("Error de validación")
                .mensaje("La solicitud contiene campos inválidos")
                .ruta(request.getRequestURI())
                .erroresValidacion(erroresValidacion)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpo);
    }
}
