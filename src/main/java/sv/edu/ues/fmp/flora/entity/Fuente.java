package sv.edu.ues.fmp.flora.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sv.edu.ues.fmp.flora.entity.enums.TipoFuente;

/**
 * Fuente bibliografica o de conocimiento. Corresponde a la tabla {@code fuente}.
 * La bandera de estado de esta tabla es {@code activa} (femenino), no {@code activo}.
 * La columna {@code tipo_fuente} se persiste como texto (EnumType.STRING) sobre un
 * varchar(22) protegido por CHECK constraint.
 */
@Entity
@Table(name = "fuente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fuente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_fuente", nullable = false, updatable = false)
    private Long idFuente;

    @Column(name = "titulo", nullable = false, length = 300)
    private String titulo;

    @Column(name = "autor", length = 250)
    private String autor;

    @Column(name = "anio")
    private Short anio;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_fuente", nullable = false, length = 22)
    private TipoFuente tipoFuente;

    @Column(name = "url", length = 1000)
    private String url;

    @Column(name = "referencia_bibliografica", columnDefinition = "text")
    private String referenciaBibliografica;

    @Builder.Default
    @Column(name = "activa", nullable = false)
    private Boolean activa = true;
}
