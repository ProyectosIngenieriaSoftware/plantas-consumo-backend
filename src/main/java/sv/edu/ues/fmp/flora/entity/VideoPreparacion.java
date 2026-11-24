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
 * Entidad que representa un video asociado a una preparacion de consumo.
 * Almacenado unicamente como URL (cadena de texto).
 */
@Entity
@Table(name = "video_preparacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoPreparacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_video_preparacion", nullable = false, updatable = false)
    private Long idVideoPreparacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_preparacion", nullable = false, updatable = false)
    private PreparacionConsumo preparacion;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;

    @Column(name = "url_video", nullable = false, length = 1000)
    private String urlVideo;

    @Column(name = "duracion_segundos")
    private Integer duracionSegundos;

    @Column(name = "creditos", length = 250)
    private String creditos;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /** Asignado por PostgreSQL (CURRENT_TIMESTAMP) y leido por Hibernate al insertar. */
    @Generated(event = EventType.INSERT)
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;
}

