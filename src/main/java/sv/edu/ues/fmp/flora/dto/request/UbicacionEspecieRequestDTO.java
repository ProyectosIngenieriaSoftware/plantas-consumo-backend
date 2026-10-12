package sv.edu.ues.fmp.flora.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UbicacionEspecieRequestDTO {

    @NotNull(message = "El ID de la especie es obligatorio")
    @Positive(message = "El ID de la especie debe ser un número válido mayor a cero")
    private Long idEspecie;

    @NotNull(message = "El ID del distrito es obligatorio")
    @Positive(message = "El ID del distrito debe ser un número válido mayor a cero")
    private Long idDistrito;

    @NotBlank(message = "El nombre del lugar no puede estar vacío")
    @Size(max = 200, message = "El nombre del lugar no debe exceder 200 caracteres")
    private String nombreLugar;

    // Validación de coordenadas reales
    @DecimalMin(value = "-90.0", message = "La latitud debe ser mayor o igual a -90")
    @DecimalMax(value = "90.0", message = "La latitud debe ser menor o igual a 90")
    private BigDecimal latitud;

    @DecimalMin(value = "-180.0", message = "La longitud debe ser mayor o igual a -180")
    @DecimalMax(value = "180.0", message = "La longitud debe ser menor o igual a 180")
    private BigDecimal longitud;

    private String descripcion;

    // Evita que metan fechas que aún no han pasado
    @PastOrPresent(message = "La fecha de observación no puede ser una fecha futura")
    private LocalDate fechaObservacion;
}