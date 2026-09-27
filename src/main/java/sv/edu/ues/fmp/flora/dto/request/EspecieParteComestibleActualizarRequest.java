package sv.edu.ues.fmp.flora.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos que entran al actualizar (PUT) una parte comestible.
 * <p>
 * Es un DTO aparte de {@link EspecieParteComestibleRequest} precisamente para
 * que no tenga {@code idPartePlanta}: la parte asociada no puede cambiarse
 * despues de creada, porque preparaciones, aportes nutricionales, epocas de
 * cosecha, beneficios, imagenes y videos cuelgan de este registro, y cambiar
 * "Arilo" por "Semilla" los moveria en silencio a otra parte. Si un cliente
 * envia {@code idPartePlanta} en el cuerpo, Jackson lo ignora por no existir
 * el campo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecieParteComestibleActualizarRequest {

    private String descripcion;

    private String advertencias;
}
