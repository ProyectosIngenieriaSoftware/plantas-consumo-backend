package sv.edu.ues.fmp.flora.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Datos editables de una relación existente; su clave se recibe únicamente en la URL. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecieHabitatActualizarRequest {
    /** Texto opcional; null o solo espacios elimina la observación. */
    private String observacion;
}
