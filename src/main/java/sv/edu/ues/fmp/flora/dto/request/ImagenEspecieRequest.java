package sv.edu.ues.fmp.flora.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import sv.edu.ues.fmp.flora.entity.enums.TipoImagen;

/** Datos editables de una imagen; la especie y el estado se gestionan por las rutas. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImagenEspecieRequest {
    private Long idEspecieParte;

    @NotNull(message = "El tipo de imagen es obligatorio")
    private TipoImagen tipoImagen;

    @NotBlank(message = "La URL de la imagen es obligatoria")
    @Size(max = 1000, message = "La URL no puede exceder 1000 caracteres")
    // Pattern acepta null: NotBlank valida la ausencia. Ambos errores apuntan a urlImagen.
    @Pattern(regexp = "^https?://([\\w-]+\\.)+[a-zA-Z]{2,}(:\\d{1,5})?(/[^\\s]*)?$",
            message = "La URL de la imagen debe tener un formato válido (http:// o https://)")
    private String urlImagen;

    @Size(max = 200, message = "El título no puede exceder 200 caracteres")
    private String titulo;

    private String descripcion;

    @NotBlank(message = "El texto alternativo es obligatorio")
    @Size(max = 300, message = "El texto alternativo no puede exceder 300 caracteres")
    private String textoAlternativo;

    @Size(max = 250, message = "Los créditos no pueden exceder 250 caracteres")
    private String creditos;

    /** Omitido o nulo equivale a false, también al reemplazar mediante PUT. */
    private Boolean esPrincipal;

}
