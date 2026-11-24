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
@Table(name = "distrito")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Distrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_distrito", nullable = false, updatable = false)
    private Long idDistrito;

    @Column(name = "id_municipio", nullable = false)
    private Long idMunicipio;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}