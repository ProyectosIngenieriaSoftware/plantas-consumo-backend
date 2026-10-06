package sv.edu.ues.fmp.flora.entity;

import jakarta.persistence.*;
import lombok.*;


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