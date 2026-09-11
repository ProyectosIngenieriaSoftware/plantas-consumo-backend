package sv.edu.ues.fmp.flora.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Corresponde a la tabla municipio (segundo nivel territorial).
 * Cada municipio pertenece a un departamento.
 * Restricción uk_municipio_departamento_nombre: el nombre es único
 * dentro de su departamento, no globalmente.
 * Puede haber dos municipios llamados igual si están en departamentos distintos.
 * Esa validación tiene que hacerse en el servicio:
 */
@Entity
@Table(name = "municipio")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Municipio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_municipio", updatable = false)
    private Long idMunicipio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_departamento", nullable = false)
    private Departamento departamento;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}