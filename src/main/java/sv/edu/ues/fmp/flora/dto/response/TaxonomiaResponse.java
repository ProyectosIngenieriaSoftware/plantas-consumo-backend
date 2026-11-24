package sv.edu.ues.fmp.flora.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Clasificacion taxonomica tal como sale de la API, anidada dentro de
 * {@link EspecieResponse}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxonomiaResponse {

    private Long idTaxonomia;
    private String reino;
    private String division;
    private String clase;
    private String ordenTaxonomico;
    private String familia;
    private String genero;
    private String especieTaxonomica;
    private String subespecie;
}
