package sv.edu.ues.fmp.flora.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "especie_fuente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecieFuente {

    @EmbeddedId
    private EspecieFuenteId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idEspecie")
    @JoinColumn(name = "id_especie", nullable = false)
    private Especie especie;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idFuente")
    @JoinColumn(name = "id_fuente", nullable = false)
    private Fuente fuente;

    @Column(name = "observacion", columnDefinition = "text")
    private String observacion;
}