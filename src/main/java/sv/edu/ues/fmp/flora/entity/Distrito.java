package sv.edu.ues.fmp.flora.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "distrito")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Distrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_distrito")
    private Long idDistrito;

    @Column(name = "id_municipio")
    private Long idMunicipio;

    @Column(name = "nombre", length = 150)
    private String nombre;

    @Column(name = "activo")
    private Boolean activo;
}