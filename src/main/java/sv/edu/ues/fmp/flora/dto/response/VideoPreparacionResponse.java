package sv.edu.ues.fmp.flora.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Representacion publica de un video de preparacion. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoPreparacionResponse {

    private Long idVideoPreparacion;
    private Long idPreparacion;
    private String titulo;
    private String descripcion;
    private String urlVideo;
    private Integer duracionSegundos;
    private String creditos;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
}

