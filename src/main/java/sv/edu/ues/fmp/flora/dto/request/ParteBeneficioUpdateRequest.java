package sv.edu.ues.fmp.flora.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ParteBeneficioUpdateRequest {

    @NotNull(message = "La fuente es obligatoria")
    @Positive(message = "El id de la fuente debe ser positivo")
    private Long idFuente;

    @NotBlank(message = "La observación es obligatoria y no puede estar vacía")
    private String observacion;

    private Boolean activa;

    @JsonSetter(value = "activa", nulls = Nulls.FAIL)
    public void setActiva(Boolean activa) {
        this.activa = activa;
    }
}