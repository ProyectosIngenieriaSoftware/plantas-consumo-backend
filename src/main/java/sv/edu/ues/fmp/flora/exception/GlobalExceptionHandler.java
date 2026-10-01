package sv.edu.ues.fmp.flora.exception;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.InvalidNullException;
import tools.jackson.databind.exc.MismatchedInputException;

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

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(cuerpo);
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

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(cuerpo);
    }

    /**
     * Manejo específico para duplicados de Beneficio.
     */
    @ExceptionHandler(DatoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> manejarDatoDuplicado(
            DatoDuplicadoException ex,
            HttpServletRequest request) {

        ErrorResponse cuerpo = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .estado(HttpStatus.CONFLICT.value())
                .error("Conflicto")
                .mensaje(ex.getMessage())
                .ruta(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(cuerpo);
    }

    /**
     * Última red: una restricción de la base rechazó la operación.
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

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(cuerpo);
    }

    /**
     * Transición de estado inválida -> 409 CONFLICT.
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

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(cuerpo);
    }

    /**
     * Login fallido -> 401 UNAUTHORIZED.
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

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(cuerpo);
    }

    /**
     * ID inválido -> 400 BAD REQUEST.
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

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(cuerpo);
    }

    /**
     * Parametro de paginacion fuera de rango -> 400 BAD REQUEST.
     */
    @ExceptionHandler(ParametroInvalidoException.class)
    public ResponseEntity<ErrorResponse> manejarParametroInvalido(
            ParametroInvalidoException ex,
            HttpServletRequest request) {

        ErrorResponse cuerpo = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .estado(HttpStatus.BAD_REQUEST.value())
                .error("Parámetro inválido")
                .mensaje(ex.getMessage())
                .ruta(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(cuerpo);
    }

    /**
     * Token de recuperacion de clave invalido, usado o expirado -> 400 BAD REQUEST.
     */
    @ExceptionHandler(TokenInvalidoException.class)
    public ResponseEntity<ErrorResponse> manejarTokenInvalido(
            TokenInvalidoException ex,
            HttpServletRequest request) {

        ErrorResponse cuerpo = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .estado(HttpStatus.BAD_REQUEST.value())
                .error("Token inválido")
                .mensaje(ex.getMessage())
                .ruta(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(cuerpo);
    }

    /**
     * PathVariable o RequestParam con tipo incorrecto.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoInvalido(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        ErrorResponse cuerpo = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .estado(HttpStatus.BAD_REQUEST.value())
                .error("Parámetro inválido")
                .mensaje("El valor de '" + ex.getName()
                        + "' no tiene el formato esperado")
                .ruta(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(cuerpo);
    }

    /**
     * Errores de Bean Validation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarErroresDeValidacion(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> erroresValidacion = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            erroresValidacion.put(
                    error.getField(),
                    error.getDefaultMessage()
            );
        }

        ErrorResponse cuerpo = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .estado(HttpStatus.BAD_REQUEST.value())
                .error("Error de validación")
                .mensaje("La solicitud contiene campos inválidos")
                .ruta(request.getRequestURI())
                .erroresValidacion(erroresValidacion)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(cuerpo);
    }

    /**
     * JSON inválido, enum incorrecto, null no permitido o tipo incompatible.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarJsonInvalido(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        String mensaje =
                "El cuerpo de la solicitud no tiene un formato JSON válido";

        Throwable causa = ex.getCause();

        /*
         * Caso 1:
         * Enum inválido.
         *
         * Ejemplo:
         * "tipoBeneficio": "PRUEBA"
         */
        if (causa instanceof InvalidFormatException error) {

            String campo = nombreDelCampo(error.getPath());
            Class<?> tipo = error.getTargetType();

            if (tipo != null && tipo.isEnum()) {

                String valoresPermitidos =
                        Arrays.stream(tipo.getEnumConstants())
                                .map(Object::toString)
                                .collect(Collectors.joining(", "));
                mensaje =
                        "El valor '" + error.getValue()
                                + "' no es válido para el campo '"
                                + campo
                                + "'. Valores permitidos: "
                                + valoresPermitidos;
            } else if (campo != null) {
                mensaje =
                        "El campo '" + campo
                                + "' tiene un tipo de dato incorrecto";
            }
            /*
             * Caso 2:
             * Campo enviado explícitamente como null.
             *
             * Ejemplo:
             * "activo": null
             */
        } else if (causa instanceof InvalidNullException error) {

            String campo = nombreDelCampo(error.getPath());

            if (campo != null) {
                mensaje =
                        "El campo '" + campo
                                + "' no puede ser nulo";
            }

            /*
             * Caso 3:
             * Otro tipo incompatible.
             */
        } else if (causa instanceof MismatchedInputException error) {

            String campo = nombreDelCampo(error.getPath());

            if (campo != null) {
                mensaje =
                        "El campo '" + campo
                                + "' tiene un tipo de dato incorrecto";
            }
        }

        ErrorResponse cuerpo = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .estado(HttpStatus.BAD_REQUEST.value())
                .error("Solicitud mal formada")
                .mensaje(mensaje)
                .ruta(request.getRequestURI())
                .build();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(cuerpo);
    }

    /**
     * Ruta sin controlador (por ejemplo {@code /api/habitats/} con barra
     * final) -> 404 NOT FOUND, sin exponer el stack trace.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> manejarRutaInexistente(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        ErrorResponse cuerpo = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .estado(HttpStatus.NOT_FOUND.value())
                .error("Recurso no encontrado")
                .mensaje("La ruta solicitada no existe")
                .ruta(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(cuerpo);
    }

    /**
     * Falta un {@code @RequestParam} obligatorio -> 400 BAD REQUEST.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> manejarParametroFaltante(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        ErrorResponse cuerpo = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .estado(HttpStatus.BAD_REQUEST.value())
                .error("Parámetro requerido")
                .mensaje("Falta el parámetro obligatorio '" + ex.getParameterName() + "'")
                .ruta(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(cuerpo);
    }

    /**
     * Red de seguridad final: cualquier excepcion que no coincida con ninguno
     * de los manejadores anteriores -> 500 INTERNAL SERVER ERROR.
     * <p>
     * Es deliberadamente inespecifica: al cliente solo se le dice que algo
     * salio mal, nunca la clase de la excepcion ni su mensaje real (podria
     * filtrar detalles internos, como ya ocurre a proposito con
     * DataIntegrityViolationException). El stack trace completo si se escribe
     * en el log del servidor, que es donde hay que ir a diagnosticar un 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarExcepcionNoAnticipada(
            Exception ex,
            HttpServletRequest request) {

        log.error("Excepcion no anticipada en {} {}", request.getMethod(), request.getRequestURI(), ex);

        ErrorResponse cuerpo = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .estado(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Error interno")
                .mensaje("Ocurrió un error inesperado. Si persiste, contacta al equipo de backend.")
                .ruta(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(cuerpo);
    }

    /**
     * {@link ResponseStatusException} lanzada por algún servicio -> el código
     * que trae la propia excepción.
     * <p>
     * La convención del proyecto es lanzar sus propias excepciones
     * ({@link RecursoNoEncontradoException}, {@link RecursoDuplicadoException},
     * {@link EstadoInvalidoException}…), que ya tienen manejador aquí. Este
     * existe solo para que las {@code ResponseStatusException} que aún quedan
     * no pierdan su mensaje: sin él las atendería el controlador de errores de
     * Spring, que con {@code server.error.include-message=never} lo descarta y
     * además no responde en formato {@link ErrorResponse}.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> manejarResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request) {

        int codigo = ex.getStatusCode().value();
        String error = textoDelCodigo(codigo);
        String mensaje = ex.getReason() != null
                ? ex.getReason()
                : "La solicitud no pudo completarse (" + codigo + " " + error + ")";

        ErrorResponse cuerpo = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .estado(codigo)
                .error(error)
                .mensaje(mensaje)
                .ruta(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(cuerpo);
    }

    /**
     * Texto en español de los códigos HTTP habituales; para el resto, la frase
     * estándar en inglés de {@link HttpStatus}.
     */
    private static String textoDelCodigo(int codigo) {
        return switch (codigo) {
            case 400 -> "Solicitud incorrecta";
            case 401 -> "No autorizado";
            case 403 -> "Prohibido";
            case 404 -> "Recurso no encontrado";
            case 405 -> "Método no permitido";
            case 409 -> "Conflicto";
            case 422 -> "Entidad no procesable";
            case 500 -> "Error interno del servidor";
            case 503 -> "Servicio no disponible";
            default -> {
                HttpStatus estado = HttpStatus.resolve(codigo);
                yield estado != null ? estado.getReasonPhrase() : "Error " + codigo;
            }
        };
    }

    /**
     * Devuelve el nombre del último campo presente en la ruta de Jackson.
     */
    private static String nombreDelCampo(
            List<JacksonException.Reference> ruta) {
        for (int i = ruta.size() - 1; i >= 0; i--) {
            String nombre = ruta.get(i).getPropertyName();
            if (nombre != null) {
                return nombre;
            }
        }
        return null;
    }
}
