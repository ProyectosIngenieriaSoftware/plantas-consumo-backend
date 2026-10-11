package sv.edu.ues.fmp.flora.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Datos editables para crear o actualizar un video de una especie. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoEspecieRequest {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200, message = "El título no puede exceder 200 caracteres")
    private String titulo;

    private String descripcion;

    @NotBlank(message = "La URL del video es obligatoria")
    @Size(max = 1000, message = "La URL del video no puede exceder 1000 caracteres")
    @Pattern(regexp = "^https?://\\S+$", message = "La URL del video debe comenzar con http:// o https:// y ser válida")
    private String urlVideo;

    @Positive(message = "La duración debe ser mayor que cero")
    private Integer duracionSegundos;

    @Size(max = 250, message = "Los créditos no pueden exceder 250 caracteres")
    private String creditos;

    private Long idEspecieParte;
}
