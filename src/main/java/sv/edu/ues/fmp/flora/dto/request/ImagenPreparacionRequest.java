package sv.edu.ues.fmp.flora.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Datos para registrar o actualizar una imagen de una preparacion. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImagenPreparacionRequest {

    @NotBlank(message = "La URL de la imagen es obligatoria")
    @Size(max = 1000, message = "La URL de la imagen no puede exceder 1000 caracteres")
    @Schema(description = "URL accesible de la imagen", example = "https://ejemplo.com/imagenes/receta.jpg")
    private String urlImagen;

    @Size(max = 200, message = "El título no puede exceder 200 caracteres")
    @Schema(description = "Título descriptivo opcional", example = "Presentación del platillo terminado")
    private String titulo;

    @Schema(description = "Descripción detallada de la imagen", example = "Muestra de la porción servida con acompañamiento")
    private String descripcion;

    @NotBlank(message = "El texto alternativo es obligatorio")
    @Size(max = 300, message = "El texto alternativo no puede exceder 300 caracteres")
    @Schema(description = "Texto alternativo para accesibilidad", example = "Plato servido con la preparación lista para consumir")
    private String textoAlternativo;

    @Size(max = 250, message = "Los créditos no pueden exceder 250 caracteres")
    @Schema(description = "Autor o fuente de la imagen", example = "Fotografía propia del proyecto")
    private String creditos;

    @Schema(description = "Indica si es la imagen principal de la preparación", example = "false")
    private Boolean esPrincipal;

    private static final java.util.regex.Pattern PATRON_URL =
            java.util.regex.Pattern.compile("^https?://([\\w-]+\\.)+[a-zA-Z]{2,}(:\\d{1,5})?(/[^\\s]*)?$");

    @Schema(hidden = true)
    @JsonIgnore
    @AssertTrue(message = "La URL de la imagen debe tener un formato válido (http:// o https://)")
    public boolean isUrlValida() {
        return urlImagen != null && PATRON_URL.matcher(urlImagen.strip()).matches();
    }
}

