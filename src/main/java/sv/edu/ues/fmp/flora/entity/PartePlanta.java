package sv.edu.ues.fmp.flora.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Parte aprovechable de una planta (hoja, raiz, fruto, etc.).
 * Corresponde a la tabla {@code parte_planta}.
 * La columna {@code nombre} tiene restriccion UNIQUE en la base de datos.
 */
@Entity
@Table(name = "parte_planta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartePlanta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parte_planta", nullable = false, updatable = false)
    private Long idPartePlanta;

    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(name = "descripcion", length = 250)
    private String descripcion;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;




}
