package sv.edu.ues.fmp.flora.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "aporte_nutricional")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AporteNutricional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aporte", nullable = false, updatable = false)
    private Long idAporte;

    // Se mapea como ID simple temporalmente hasta que crees la entidad EspecieParteComestible
    @Column(name = "id_especie_parte", nullable = false)
    private Long idEspecieParte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nutriente", nullable = false)
    private Nutriente nutriente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_fuente", nullable = false)
    private Fuente fuente;

    @Column(name = "cantidad", precision = 12, scale = 4, nullable = false)
    private BigDecimal cantidad;

    @Column(name = "unidad_medida", length = 30, nullable = false)
    private String unidadMedida;

    @Column(name = "porcion_referencia", length = 100, nullable = false)
    private String porcionReferencia;

    @Column(name = "observacion", columnDefinition = "text")
    private String observacion;
}