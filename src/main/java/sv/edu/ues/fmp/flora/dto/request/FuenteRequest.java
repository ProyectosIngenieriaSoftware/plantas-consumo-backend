package sv.edu.ues.fmp.flora.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sv.edu.ues.fmp.flora.entity.enums.TipoFuente;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuenteRequest {
    // lo que entra

    @NotBlank(message = "El título es obligatorio") // (null,""," ")
    @Size(max = 300, message = "El título no puede exceder 300 caracteres")
    private String titulo;

    @Size(max = 250, message = "El autor no puede exceder 250 caracteres")
    private String autor;

    private Short anio;

    @NotNull(message = "El tipo de fuente es obligatorio")
    private TipoFuente tipoFuente;

    @Size(max = 1000, message = "La url no puede exceder 1000 caracteres")
    private String url;

    private String referenciaBibliografica;
}