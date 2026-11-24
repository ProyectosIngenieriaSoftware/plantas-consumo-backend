package sv.edu.ues.fmp.flora.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sv.edu.ues.fmp.flora.entity.enums.TipoFuente;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecieFuenteResponse {

    private Long idEspecie;
    private Long idFuente;
    private String tituloFuente;
    private TipoFuente tipoFuente;
    private String observacion;
}