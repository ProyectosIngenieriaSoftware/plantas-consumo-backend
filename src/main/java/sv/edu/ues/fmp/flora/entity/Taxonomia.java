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
