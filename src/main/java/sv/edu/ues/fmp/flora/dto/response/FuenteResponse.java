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
public class FuenteResponse {
    // lo que sale
    private Long idFuente;
    private String titulo;
    private String autor;
    private Short anio;
    private TipoFuente tipoFuente;
    private String url;
    private String referenciaBibliografica;
    private Boolean activa;
}
