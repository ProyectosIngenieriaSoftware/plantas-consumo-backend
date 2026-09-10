package sv.edu.ues.fmp.flora.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Captura en un solo lugar las excepciones de toda la API y las convierte en
 * respuestas HTTP con un cuerpo {@link ErrorResponse} uniforme.
 * Gracias a esto los controladores quedan libres de bloques try/catch.
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
