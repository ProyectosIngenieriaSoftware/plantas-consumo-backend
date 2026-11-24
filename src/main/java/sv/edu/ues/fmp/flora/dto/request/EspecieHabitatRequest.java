package sv.edu.ues.fmp.flora.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Datos para asociar un hábitat; la especie se obtiene de la URL. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecieHabitatRequest {

    @NotNull(message = "El hábitat es obligatorio")
    private Long idHabitat;

    /** Texto opcional, sin limite adicional al definido por PostgreSQL. */
    private String observacion;
}
