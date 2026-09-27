package sv.edu.ues.fmp.flora.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos que entran al registrar una parte comestible en una especie.
 * <p>
 * No lleva {@code idEspecie} (viaja en la ruta anidada y el servicio la
 * resuelve) ni {@code activa} (la baja y el alta logicas tienen endpoints
 * propios). Para editar un registro existente se usa
 * {@link EspecieParteComestibleActualizarRequest}, que no admite cambiar la parte.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecieParteComestibleRequest {

    @NotNull(message = "La parte de la planta es obligatoria")
    private Long idPartePlanta;

    private String descripcion;

    private String advertencias;
}
