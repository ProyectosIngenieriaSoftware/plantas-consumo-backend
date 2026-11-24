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
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "video_especie")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoEspecie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_video", nullable = false, updatable = false)
    private Long idVideo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_especie", nullable = false)
    private Especie especie;

    @Column(name = "id_especie_parte")
    private Long idEspecieParte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "id_especie_parte", referencedColumnName = "id_especie_parte",
                    insertable = false, updatable = false),
            @JoinColumn(name = "id_especie", referencedColumnName = "id_especie",
                    insertable = false, updatable = false)
    })
    private EspecieParteComestible especieParte;

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

    @Generated(event = EventType.INSERT)
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;
}
