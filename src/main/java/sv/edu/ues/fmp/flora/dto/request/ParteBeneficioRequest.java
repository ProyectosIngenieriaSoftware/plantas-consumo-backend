package sv.edu.ues.fmp.flora.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class ParteBeneficioRequest {

    @NotNull(message = "La parte comestible es obligatoria")
    @Positive(message = "El id de la parte comestible debe ser positivo")
    private Long idEspecieParte;

    @NotNull(message = "El beneficio es obligatorio")
    @Positive(message = "El id del beneficio debe ser positivo")
    private Long idBeneficio;

    @NotNull(message = "La fuente es obligatoria")
    @Positive(message = "El id de la fuente debe ser positivo")
    private Long idFuente;

    @NotBlank(message = "La observación es obligatoria y no puede estar vacía")
    private String observacion;
}