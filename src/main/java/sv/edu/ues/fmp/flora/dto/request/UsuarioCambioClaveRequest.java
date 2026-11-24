package sv.edu.ues.fmp.flora.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioCambioClaveRequest(
        @NotBlank(message = "La clave actual es obligatoria")
        String claveActual,

        @NotBlank(message = "La clave nueva es obligatoria")
        // El maximo de 72 lo impone BCrypt, que trunca a 72 bytes: permitir mas
        // seria aceptar en silencio una clave cuyo final no se comprueba.
        @Size(min = 8, max = 72, message = "La clave debe tener entre 8 y 72 caracteres")
        // Misma politica que al crear el usuario (ver UsuarioCreationRequest):
        // (?=.*[A-Z]) una mayuscula, (?=.*\d) un digito y (?=.*[^A-Za-z0-9])
        // un caracter que no sea letra ni numero. El .+ acepta el resto; la
        // longitud la impone el @Size. No se exige minuscula a proposito.
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "La clave debe tener al menos una mayúscula, un número y un carácter especial")
        String claveNueva
) {
}
