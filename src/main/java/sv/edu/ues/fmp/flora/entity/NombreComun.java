package sv.edu.ues.fmp.flora.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "nombre_comun")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NombreComun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nombre_comun", nullable = false, updatable = false)
    private Long idNombreComun;

    /**
     * Especie a la que pertenece el nombre. Es obligatoria y no cambia una vez
     * creado el registro: para mover un nombre de especie se crea otro.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_especie", nullable = false)
    private Especie especie;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    /** Zona donde se usa el nombre. Es opcional: un nombre puede ser nacional. */
    @Column(name = "region", length = 150)
    private String region;

    @Builder.Default
    @Column(name = "es_principal", nullable = false)
    private Boolean esPrincipal = false;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
