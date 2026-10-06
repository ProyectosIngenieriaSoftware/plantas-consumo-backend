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


@Entity
@Table(name = "especie_parte_comestible")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecieParteComestible {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_especie_parte", nullable = false, updatable = false)
    private Long idEspecieParte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_especie", nullable = false)
    private Especie especie;

    /** Inmutable por regla de negocio (ver Javadoc de la clase). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_parte_planta", nullable = false)
    private PartePlanta partePlanta;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;

    @Column(name = "advertencias", columnDefinition = "text")
    private String advertencias;

    @Builder.Default
    @Column(name = "activa", nullable = false)
    private Boolean activa = true;
}
