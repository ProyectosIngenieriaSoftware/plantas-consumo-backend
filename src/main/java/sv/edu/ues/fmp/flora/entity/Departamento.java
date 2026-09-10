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

/**
 * Departamento geografico. Corresponde a la tabla {@code departamento}.
 * La columna {@code nombre} tiene restriccion UNIQUE en la base de datos.
 */
@Entity
@Table(name = "departamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Departamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_departamento", nullable = false, updatable = false)
    private Long idDepartamento;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
