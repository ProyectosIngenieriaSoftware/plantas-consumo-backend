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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_especie_parte", nullable = false)
    private EspecieParteComestible especieParteComestible;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nutriente", nullable = false)
    private Nutriente nutriente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_fuente")
    private Fuente fuente;

    @Column(name = "cantidad", precision = 12, scale = 4)
    private BigDecimal cantidad;

    @Column(name = "unidad_medida", length = 30)
    private String unidadMedida;

    @Column(name = "porcion_referencia", length = 100, nullable = false)
    private String porcionReferencia;

    @Column(name = "observacion", columnDefinition = "text")
    private String observacion;

    /**
     * Baja logica, como en el resto del catalogo. La columna la agrega
     * {@code V21}: {@code boolean NOT NULL DEFAULT true}, asi que las filas
     * que ya estaban quedaron activas.
     * <p>
     * El {@code @Builder.Default} cubre la construccion por builder --que es
     * por donde pasa {@code AporteNutricionalMapper.toEntity}-- pero
     * <strong>no</strong> es de lo que depende que un aporte nuevo nazca
     * activo: el mapper lo fija a {@code true} a mano. La razon es la que se
     * encontro en beneficio: un {@code @Builder.Default} no sobrevive a la
     * deserializacion de Jackson, porque Jackson construye con el constructor
     * vacio y ese camino no pasa por el builder. Aqui no se nota, porque
     * {@code AporteNutricionalRequest} <strong>no</strong> tiene campo
     * {@code activo} --el cliente no puede pedir el estado al crear, que es
     * justo lo que pedia el cambio-- pero dejarlo fijado en el mapper evita
     * que el dia que alguien agregue el campo al DTO herede el problema.
     */
    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}