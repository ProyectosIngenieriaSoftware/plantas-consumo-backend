package sv.edu.ues.fmp.flora.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sv.edu.ues.fmp.flora.entity.enums.CategoriaNutriente;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NutrienteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
    private String nombre;

    @NotNull(message = "La categoría del nutriente es obligatoria")
    private CategoriaNutriente categoria;

    private String descripcion;
}
