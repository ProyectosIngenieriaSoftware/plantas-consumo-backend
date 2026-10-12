package sv.edu.ues.fmp.flora.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Datos para registrar o actualizar un video de una preparacion. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoPreparacionRequest {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200, message = "El título no puede exceder 200 caracteres")
    @Schema(description = "Título del video", example = "Elaboración paso a paso de la receta")
    private String titulo;

    @Schema(description = "Descripción opcional del contenido del video", example = "Explicación del tiempo de cocción y recomendaciones")
    private String descripcion;

    @NotBlank(message = "La URL del video es obligatoria")
    @Size(max = 1000, message = "La URL del video no puede exceder 1000 caracteres")
    @Pattern(regexp = "^https?://\\S+$", message = "La URL del video debe comenzar con http:// o https:// y ser válida")
    @Schema(description = "URL accesible del video", example = "https://www.youtube.com/watch?v=ejemplo123")
    private String urlVideo;

    @Positive(message = "La duración debe ser mayor que cero")
    @Schema(description = "Duración total del video en segundos", example = "180")
    private Integer duracionSegundos;

    @Size(max = 250, message = "Los créditos no pueden exceder 250 caracteres")
    @Schema(description = "Autor o créditos del video", example = "Canal de Cocina Tradicional")
    private String creditos;
}

