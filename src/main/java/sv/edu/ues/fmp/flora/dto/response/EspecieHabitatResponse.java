package sv.edu.ues.fmp.flora.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Resumen de la relación con los IDs y nombres de sus entidades asociadas. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecieHabitatResponse {
    private Long idEspecie;
    private Long idHabitat;
    private String nombreCientificoEspecie;
    private String nombreHabitat;
    private String observacion;
}
