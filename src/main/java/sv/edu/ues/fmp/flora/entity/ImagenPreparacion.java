package sv.edu.ues.fmp.flora.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

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

/**
 * Entidad que representa una imagen asociada a una preparacion de consumo.
 * Almacenada unicamente como URL (cadena de texto).
 * La base de datos garantiza una unica imagen principal activa por preparacion
 * mediante el indice parcial uk_imagen_principal_preparacion.
 */
@Entity
@Table(name = "imagen_preparacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImagenPreparacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imagen_preparacion", nullable = false, updatable = false)
    private Long idImagenPreparacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_preparacion", nullable = false, updatable = false)
    private PreparacionConsumo preparacion;

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

    /** Asignado por PostgreSQL (CURRENT_TIMESTAMP) y leido por Hibernate al insertar. */
    @Generated(event = EventType.INSERT)
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;
}

