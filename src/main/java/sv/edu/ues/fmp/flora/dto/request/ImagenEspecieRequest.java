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

    // Mismo patrón que FuenteRequest.URL_VALIDA, que es privado en ese DTO.
    private static final java.util.regex.Pattern URL_VALIDA =
            java.util.regex.Pattern.compile("^https?://([\\w-]+\\.)+[a-zA-Z]{2,}(:\\d{1,5})?(/[^\\s]*)?$");

    /** Comprueba el formato HTTP(S) sobre el mismo valor que guardará el mapper. */
    @io.swagger.v3.oas.annotations.media.Schema(hidden = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @AssertTrue(message = "La URL de la imagen debe tener un formato válido (http:// o https://)")
    public boolean isUrlValida() {
        return urlImagen != null && URL_VALIDA.matcher(urlImagen.strip()).matches();
    }
}
