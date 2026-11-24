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
    private String especieNombreCientifico;
    private String parteNombre;

    private Long idNutriente;
    private String nutrienteNombre;

    private Long idFuente;
    private String fuenteTitulo;

    private BigDecimal cantidad;
    private String unidadMedida;
    private String porcionReferencia;
    private String observacion;

    /**
     * Baja logica: {@code false} es un aporte retirado. Sale en la respuesta
     * para que la pantalla de administracion pueda distinguir lo vigente de lo
     * retirado --y ofrecer el {@code PATCH /{id}/activar}-- sin tener que
     * pedir el detalle de cada uno.
     */
    private Boolean activo;
}

