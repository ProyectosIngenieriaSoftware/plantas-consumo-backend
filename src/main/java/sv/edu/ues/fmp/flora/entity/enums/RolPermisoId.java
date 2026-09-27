package sv.edu.ues.fmp.flora.entity.enums;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clave primaria compuesta de la tabla de union {@code rol_permiso}
 * (PRIMARY KEY (id_rol, id_permiso)).
 * <p>
 * {@code equals} y {@code hashCode} se implementan a mano sobre ambos campos
 * porque JPA los exige en las claves compuestas para determinar la identidad
 * de la entidad dentro del contexto de persistencia.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolPermisoId implements Serializable {

    @Column(name = "id_rol", nullable = false)
    private Long idRol;

    @Column(name = "id_permiso", nullable = false)
    private Long idPermiso;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RolPermisoId that = (RolPermisoId) o;
        return Objects.equals(idRol, that.idRol)
                && Objects.equals(idPermiso, that.idPermiso);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idRol, idPermiso);
    }
}
