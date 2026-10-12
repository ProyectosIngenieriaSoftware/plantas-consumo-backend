package sv.edu.ues.fmp.flora.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Datos expuestos de un video asociado a una especie. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoEspecieResponse {

    private Long idVideo;
    private Long idEspecie;
    private Long idEspecieParte;
    private String titulo;
    private String descripcion;
    private String urlVideo;
    private Integer duracionSegundos;
    private String creditos;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
}
