package sv.edu.ues.fmp.flora.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParteBeneficioResponse {
    private Long idEspecieParte;
    private Long idBeneficio;
    private Long idFuente;
    private String observacion;
    private Boolean activa;
}
