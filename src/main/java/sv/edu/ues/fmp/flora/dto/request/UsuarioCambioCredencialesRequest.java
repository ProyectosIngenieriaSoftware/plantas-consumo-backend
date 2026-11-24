package sv.edu.ues.fmp.flora.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * El cuerpo de {@code PUT /api/usuarios/{id}/credenciales}: el dueño cambia su
 * propio correo y su propio nombre de usuario.
 *
 * <h2>Por que existe</h2>
 * Los dos campos estaban en {@link UsuarioUpdateRequest}, la edicion por
 * administrador, y de ahi salia una suplantacion: {@code POST
 * /api/usuarios/recuperar-clave} es publico y resuelve la cuenta por el correo,
 * asi que quien pudiera reescribir el correo de otra cuenta podia apuntarlo a
 * su buzon y pedir el restablecimiento. Moverlos aqui cierra la cadena para
 * todas las cuentas a la vez: ya no hay ninguna ruta por la que alguien cambie
 * las credenciales de identidad de otro.
 *
 * <h2>Por que pide la clave actual</h2>
 * Mismo criterio que {@link UsuarioCambioClaveRequest}: el token puede estar en
 * una sesion abierta y sin vigilancia, y cambiar el correo de la cuenta es
 * justo la operacion que decide a donde llega el enlace de recuperacion. La
 * clave es la prueba de que quien pide el cambio es la persona y no solo el
 * navegador. El guard de propiedad --que el {@code id} de la ruta sea el del
 * token-- lo pone el {@code @PreAuthorize} del controlador; esta clase es la
 * segunda barrera, no la primera.
 *
 * <h2>Las validaciones son las de la creacion</h2>
 * Las mismas reglas de {@code UsuarioCreationRequest} para los dos campos: si
 * un correo o un nombre de usuario no se podian dar de alta, tampoco se pueden
 * poner despues. La unicidad no se puede comprobar con una anotacion --hay que
 * consultar la base-- y la resuelve el servicio con un 409.
 */
public record UsuarioCambioCredencialesRequest(
        @NotBlank(message = "La clave actual es obligatoria")
        String claveActual,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede exceder 150 caracteres")
        String correo,

        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 4, max = 50, message = "El nombre de usuario debe tener entre 4 y 50 caracteres")
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]+$",
                message = "El nombre de usuario solo puede contener letras, números, puntos, guiones y guiones bajos")
        String nombreUsuario
) {
    public UsuarioCambioCredencialesRequest {
        // La clave NO se recorta: los espacios de una clave son parte de la
        // clave, al contrario que los de un correo escrito a mano.
        correo = correo == null ? null : correo.trim();
        nombreUsuario = nombreUsuario == null ? null : nombreUsuario.trim();
    }
}
