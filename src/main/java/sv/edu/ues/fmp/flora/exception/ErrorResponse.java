package sv.edu.ues.fmp.flora.exception;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuerpo uniforme de todas las respuestas de error de la API.
 * Tener un unico formato permite que el cliente (y Postman) siempre lea el
 * error en los mismos campos, sea un 404, un 409 o un 400 de validacion.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime timestamp;
    private int estado;
    private String error;
    private String mensaje;
    private String ruta;

    /** Solo se llena en errores de validacion: campo -> mensaje. */
    private Map<String, String> erroresValidacion;
}
