package sv.edu.ues.fmp.flora.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioCreationRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String nombres,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
        String apellidos,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede exceder 150 caracteres")
        String correo,

        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 4, max = 50, message = "El nombre de usuario debe tener entre 4 y 50 caracteres")
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]+$",
                message = "El nombre de usuario solo puede contener letras, números, puntos, guiones y guiones bajos")
        String nombreUsuario,

        @NotBlank(message = "La clave es obligatoria")
        // El maximo de 72 lo impone BCrypt, que trunca a 72 bytes: permitir mas
        // seria aceptar en silencio una clave cuyo final no se comprueba.
        @Size(min = 8, max = 72, message = "La clave debe tener entre 8 y 72 caracteres")
        // Tres miradas al frente, cada una sobre la clave completa:
        // (?=.*[A-Z]) una mayuscula, (?=.*\d) un digito y (?=.*[^A-Za-z0-9])
        // un caracter que no sea letra ni numero (!@#$%&*, acentos, ñ…).
        // El .+ final acepta el resto; la longitud la impone el @Size de arriba.
        // No se exige minuscula a proposito: la politica acordada no la pide,
        // asi que "MIAVE2026!" es valida.
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "La clave debe tener al menos una mayúscula, un número y un carácter especial")
        String clave
) {
    public UsuarioCreationRequest {
        nombres = nombres == null ? null : nombres.trim();
        apellidos = apellidos == null ? null : apellidos.trim();
        correo = correo == null ? null : correo.trim();
        nombreUsuario = nombreUsuario == null ? null : nombreUsuario.trim();
    }
}
