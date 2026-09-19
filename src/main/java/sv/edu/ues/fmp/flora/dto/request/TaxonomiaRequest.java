package sv.edu.ues.fmp.flora.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos taxonomicos que viajan anidados dentro de {@link EspecieRequest}.
 * No existe un POST propio de taxonomia: {@code especie.id_taxonomia} es NOT
 * NULL y UNIQUE, asi que una taxonomia sin especie seria un registro huerfano
 * que el modelo no contempla.
 * <p>
 * {@code reino} es opcional: si llega null o vacio el mapper asigna "Plantae",
 * que es el unico valor que admite el CHECK de la tabla.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxonomiaRequest {

    @Size(max = 100, message = "El reino no puede exceder 100 caracteres")
    private String reino;

    @Size(max = 100, message = "La división no puede exceder 100 caracteres")
    private String division;

    @Size(max = 100, message = "La clase no puede exceder 100 caracteres")
    private String clase;

    @Size(max = 100, message = "El orden taxonómico no puede exceder 100 caracteres")
    private String ordenTaxonomico;

    @Size(max = 100, message = "La familia no puede exceder 100 caracteres")
    private String familia;

    @NotBlank(message = "El género es obligatorio")
    @Size(max = 100, message = "El género no puede exceder 100 caracteres")
    private String genero;

    @NotBlank(message = "La especie taxonómica es obligatoria")
    @Size(max = 150, message = "La especie taxonómica no puede exceder 150 caracteres")
    private String especieTaxonomica;

    @Size(max = 150, message = "La subespecie no puede exceder 150 caracteres")
    private String subespecie;
}
