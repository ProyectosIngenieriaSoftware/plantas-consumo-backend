package sv.edu.ues.fmp.flora.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UbicacionEspecieRequest {

    @NotNull(message = "El ID de la especie es obligatorio")
    @Positive(message = "El ID de la especie debe ser un número válido mayor a cero")
    private Long idEspecie;

    @NotNull(message = "El ID del distrito es obligatorio")
    @Positive(message = "El ID del distrito debe ser un número válido mayor a cero")
    private Long idDistrito;

    @NotBlank(message = "El nombre del lugar no puede estar vacío")
    @Size(max = 200, message = "El nombre del lugar no debe exceder 200 caracteres")
    private String nombreLugar;

    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin(value = "-90.0", message = "La latitud debe ser mayor o igual a -90")
    @DecimalMax(value = "90.0", message = "La latitud debe ser menor o igual a 90")
    private BigDecimal latitud;

    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = "-180.0", message = "La longitud debe ser mayor o igual a -180")
    @DecimalMax(value = "180.0", message = "La longitud debe ser menor o igual a 180")
    private BigDecimal longitud;

    private String descripcion;

    @PastOrPresent(message = "La fecha de observación no puede ser una fecha futura")
    private LocalDate fechaObservacion;
}
