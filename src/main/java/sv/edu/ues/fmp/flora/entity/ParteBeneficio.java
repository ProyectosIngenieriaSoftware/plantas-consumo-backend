package sv.edu.ues.fmp.flora.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.domain.Persistable;

import sv.edu.ues.fmp.flora.entity.enums.ParteBeneficioId;

@Entity
@Table(name = "parte_beneficio", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParteBeneficio implements Persistable<ParteBeneficioId> {

    @EmbeddedId
    private ParteBeneficioId id;

    @MapsId("idBeneficio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_beneficio", nullable = false)
    private Beneficio beneficio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_fuente")
    private Fuente fuente;

    @Column(name = "observacion", columnDefinition = "text")
    private String observacion;

    @Builder.Default
    @Column(name = "activa", nullable = false)
    private Boolean activa = true;
    
    @Transient
    @Builder.Default
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean nueva = true;

    @Override
    public boolean isNew() {
        return nueva;
    }

    @PostLoad
    @PostPersist
    private void marcarComoPersistida() {
        nueva = false;
    }
}
