package sv.edu.ues.fmp.flora.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AporteNutricionalRequest {

    @NotNull(message = "El ID de la parte comestible de la especie es obligatorio")
    private Long idEspecieParte;

    @NotNull(message = "El ID del nutriente es obligatorio")
    private Long idNutriente;

    @NotNull(message = "El ID de la fuente es obligatorio")
    private Long idFuente;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.0", inclusive = true, message = "La cantidad debe ser 0 o mayor")
    private BigDecimal cantidad;

    @NotBlank(message = "La unidad de medida es obligatoria")
    @Size(max = 30, message = "La unidad de medida no puede exceder 30 caracteres")
    private String unidadMedida;

    @NotBlank(message = "La porción de referencia es obligatoria")
    @Size(max = 100, message = "La porción de referencia no puede exceder 100 caracteres")
    private String porcionReferencia;

    private String observacion;
}