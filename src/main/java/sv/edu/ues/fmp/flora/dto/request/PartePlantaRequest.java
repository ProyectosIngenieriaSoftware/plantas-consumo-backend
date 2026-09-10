package sv.edu.ues.fmp.flora.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartePlantaRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede exceder 80 caracteres")
    private String nombre;

    @Size(max = 250, message = "La descripción no puede exceder 250 caracteres")
    private String descripcion;
}
