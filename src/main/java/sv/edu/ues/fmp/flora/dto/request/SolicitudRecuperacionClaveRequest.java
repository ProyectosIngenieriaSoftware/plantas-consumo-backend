package sv.edu.ues.fmp.flora.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SolicitudRecuperacionClaveRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String correo
) {
    public SolicitudRecuperacionClaveRequest {
        correo = correo == null ? null : correo.trim();
    }
}
