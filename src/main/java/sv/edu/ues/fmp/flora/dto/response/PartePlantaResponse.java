package sv.edu.ues.fmp.flora.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartePlantaResponse {
    //lo que sale
    private Long idPartePlanta;
    private String nombre;
    private String descripcion;
    private Boolean activo;
}
