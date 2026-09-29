package sv.edu.ues.fmp.flora.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AporteNutricionalResponse {
    private Long idAporte;
    private Long idEspecieParte;

    // Devolvemos el ID y el Nombre para facilitar el trabajo al frontend
    private Long idNutriente;
    private String nutrienteNombre;

    private Long idFuente;
    private String fuenteTitulo;

    private BigDecimal cantidad;
    private String unidadMedida;
    private String porcionReferencia;
    private String observacion;
}