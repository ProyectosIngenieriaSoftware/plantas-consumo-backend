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
@Table(name = "rol")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol", nullable = false, updatable = false)
    private Long idRol;

    @Column(name = "nombre", nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(name = "descripcion", length = 250)
    private String descripcion;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /**
     * Si es uno de los roles que el sistema necesita para funcionar
     * (ADMINISTRADOR e INVESTIGADOR), de {@code V20}.
     *
     * <h2>Para que esta</h2>
     * Es el ancla de SEG-H09 R7. Las reglas que protegen el acceso de
     * administracion preguntaban si un rol <em>se llama</em> ADMINISTRADOR, y
     * el nombre es un dato que {@code PUT /api/roles/{id}} escribe: renombrarlo
     * apagaba R2, R3 y R5 de una peticion. Ahora la autoridad es esta columna,
     * que ninguna ruta escribe.
     *
     * <h2>Quien la escribe</h2>
     * Solo una migracion. No esta en {@code RolRequest}, y ni
     * {@code RolMapper.toEntity} ni {@code .updateEntity} la tocan, asi que un
     * rol creado por la API nace en {@code false} --el {@code @Builder.Default}
     * y el DEFAULT de la columna dicen lo mismo-- y ningun cuerpo la puede
     * cambiar. Igual que {@code usuario.es_superadmin}.
     */
    @Builder.Default
    @Column(name = "es_sistema", nullable = false)
    private Boolean esSistema = false;
}
