package sv.edu.ues.fmp.flora.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Clasificacion taxonomica de una especie. Corresponde a la tabla {@code taxonomia}.
 * Esta tabla no tiene bandera de estado ({@code activo}/{@code activa}).
 * <p>
 * Restricciones que viven solo en la base de datos y no se expresan en JPA:
 * <ul>
 *   <li>Un CHECK constraint obliga a que {@code reino} sea 'plantae' al comparar en
 *       minusculas, por lo que el valor por defecto 'Plantae' es el unico admitido.</li>
 *   <li>Un UNIQUE NULLS NOT DISTINCT sobre
 *       ({@code reino}, {@code genero}, {@code especie_taxonomica}, {@code subespecie}),
 *       que trata los NULL como iguales entre si; JPA no puede declarar esa semantica.</li>
 * </ul>
 */
@Entity
@Table(name = "taxonomia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Taxonomia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_taxonomia", nullable = false, updatable = false)
    private Long idTaxonomia;

    @Builder.Default
    @Column(name = "reino", nullable = false, length = 100)
    private String reino = "Plantae";

    @Column(name = "division", length = 100)
    private String division;

    @Column(name = "clase", length = 100)
    private String clase;

    @Column(name = "orden_taxonomico", length = 100)
    private String ordenTaxonomico;

    @Column(name = "familia", length = 100)
    private String familia;

    @Column(name = "genero", nullable = false, length = 100)
    private String genero;

    @Column(name = "especie_taxonomica", nullable = false, length = 150)
    private String especieTaxonomica;

    @Column(name = "subespecie", length = 150)
    private String subespecie;
}
