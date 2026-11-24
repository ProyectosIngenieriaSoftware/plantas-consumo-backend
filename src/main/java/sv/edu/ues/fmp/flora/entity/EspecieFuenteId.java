package sv.edu.ues.fmp.flora.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Builder
public class EspecieFuenteId implements Serializable {

    @Column(name = "id_especie")
    private Long idEspecie;

    @Column(name = "id_fuente")
    private Long idFuente;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EspecieFuenteId that = (EspecieFuenteId) o;
        return Objects.equals(idEspecie, that.idEspecie) &&
                Objects.equals(idFuente, that.idFuente);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idEspecie, idFuente);
    }
}