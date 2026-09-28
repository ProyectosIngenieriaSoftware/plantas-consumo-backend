package sv.edu.ues.fmp.flora.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Null;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos que entran al actualizar (PUT) una parte comestible.
 * <p>
 * Es un DTO aparte de {@link EspecieParteComestibleRequest} porque la parte
 * asociada no puede cambiarse despues de creada: preparaciones, aportes
 * nutricionales, epocas de cosecha, beneficios, imagenes y videos cuelgan de
 * este registro, y cambiar "Arilo" por "Semilla" los moveria en silencio a otra
 * parte. Si un cliente envia {@code idPartePlanta} con valor, la peticion se
 * rechaza con 400 en lugar de ignorarse, para que sepa que no se aplico.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecieParteComestibleActualizarRequest {

    private String descripcion;

    private String advertencias;

    /**
     * Existe solo para rechazar explicitamente el intento de cambiar la parte:
     * la parte es la identidad de la asociacion, y de ella cuelgan
     * preparaciones, nutrientes, cosechas y multimedia. El mapper no lo lee.
     * Oculto en Swagger para que no parezca un campo editable.
     */
    @Schema(hidden = true)
    @Null(message = "La parte de la planta no se puede modificar. Desactive esta "
            + "asociación y cree una nueva con la parte correcta.")
    private Long idPartePlanta;
}
