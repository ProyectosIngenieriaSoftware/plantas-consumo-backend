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
import sv.edu.ues.fmp.flora.entity.enums.UsuarioRolId;

import java.time.LocalDateTime;

/**
 * Asignacion de un rol a un usuario. Corresponde a la tabla de union
 * {@code usuario_rol}, cuya clave primaria es compuesta (id_usuario, id_rol)
 * y que no tiene columna de identidad autogenerada.
 * <p>
 * La columna {@code fecha_asignacion} es NOT NULL con DEFAULT CURRENT_TIMESTAMP
 * en la base; en Java se inicializa explicitamente para que el valor viaje
 * siempre en el INSERT.
 */
@Entity
@Table(name = "usuario_rol")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioRol {

    @EmbeddedId
    private UsuarioRolId id;

    @MapsId("idUsuario")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @MapsId("idRol")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @Builder.Default
    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion = LocalDateTime.now();
}
