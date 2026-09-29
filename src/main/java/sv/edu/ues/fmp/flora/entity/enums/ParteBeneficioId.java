package sv.edu.ues.fmp.flora.entity.enums;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParteBeneficioId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "id_especie_parte", nullable = false)
    private Long idEspecieParte;

    @Column(name = "id_beneficio", nullable = false)
    private Long idBeneficio;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParteBeneficioId that = (ParteBeneficioId) o;
        return Objects.equals(idEspecieParte, that.idEspecieParte)
                && Objects.equals(idBeneficio, that.idBeneficio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idEspecieParte, idBeneficio);
    }
}
