package sv.edu.ues.fmp.flora.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import sv.edu.ues.fmp.flora.entity.enums.TipoImagen;

/**
 * Imagen de una especie almacenada como URL, con baja lógica.
 * La base garantiza una principal activa mediante uk_imagen_principal_especie.
 * Las dos relaciones simples comparten la validación de pertenencia de la FK
 * compuesta fk_imagen_parte_especie; el servicio la comprueba antes de guardar.
 */
@Entity
@Table(name = "imagen_especie")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImagenEspecie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imagen", nullable = false, updatable = false)
    private Long idImagen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_especie", nullable = false, updatable = false)
    private Especie especie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_especie_parte")
    private EspecieParteComestible especieParte;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_imagen", nullable = false, length = 20)
    private TipoImagen tipoImagen;

    @Column(name = "url_imagen", nullable = false, length = 1000)
    private String urlImagen;

    @Column(name = "titulo", length = 200)
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;

    @Column(name = "texto_alternativo", nullable = false, length = 300)
    private String textoAlternativo;

    @Column(name = "creditos", length = 250)
    private String creditos;

    @Builder.Default
    @Column(name = "es_principal", nullable = false)
    private Boolean esPrincipal = false;

    @Builder.Default
    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    /** PostgreSQL asigna CURRENT_TIMESTAMP y Hibernate recupera el valor generado. */
    @Generated(event = EventType.INSERT)
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;
}
