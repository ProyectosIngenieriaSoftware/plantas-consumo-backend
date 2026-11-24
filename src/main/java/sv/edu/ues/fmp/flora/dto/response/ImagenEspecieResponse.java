package sv.edu.ues.fmp.flora.dto.response;

import java.time.LocalDateTime;
import lombok.*;
import sv.edu.ues.fmp.flora.entity.enums.TipoImagen;

/** Representación pública de la imagen sin exponer entidades ni relaciones diferidas. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImagenEspecieResponse {
    private Long idImagen;
    private Long idEspecie;
    private Long idEspecieParte;
    private TipoImagen tipoImagen;
    private String urlImagen;
    private String titulo;
    private String descripcion;
    private String textoAlternativo;
    private String creditos;
    private Boolean esPrincipal;
    private Boolean activa;
    private LocalDateTime fechaRegistro;
}
