package sv.edu.ues.fmp.flora.dto.request;

import java.time.Year;
import java.util.regex.Pattern;

import jakarta.validation.constraints.AssertTrue;
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


    private static final Pattern URL_VALIDA =
            Pattern.compile("^https?://([\\w-]+\\.)+[a-zA-Z]{2,}(:\\d{1,5})?(/[^\\s]*)?$");

    @NotBlank(message = "El título es obligatorio") // (null,""," ")
    @Size(max = 300, message = "El título no puede exceder 300 caracteres")
    private String titulo;

    @Size(max = 250, message = "El autor no puede exceder 250 caracteres")
    private String autor;

    /**
     * Primer anio que acepta la columna, tomado del CHECK
     * {@code ck_fuente_anio} de la base:
     * {@code anio IS NULL OR (anio >= 1000 AND anio <= 2100)}.
     * <p>
     * Antes esta validacion empezaba en 1, y los anios de 1 a 999 pasaban por
     * aqui para morir despues contra ese CHECK: el
     * {@code DataIntegrityViolationException} salia como un 409 con texto de
     * base de datos, cuando a un anio fuera de rango le corresponde un 400 de
     * validacion. Lo reporto QA (Diego, punto 1: el formulario acepta anios
     * que la base rechaza).
     * <p>
     * El tope superior no se copia del CHECK a proposito. La base admite hasta
     * 2100, pero una fuente no se publica en el futuro, asi que
     * {@link #isAnioValido()} sigue cortando en el anio actual: el DTO es mas
     * estricto que la base por arriba, y ahora igual de estricto por abajo.
     */
    private static final int ANIO_MINIMO = 1000;

    private Short anio;

    @NotNull(message = "El tipo de fuente es obligatorio")
    private TipoFuente tipoFuente;

    @Size(max = 1000, message = "La url no puede exceder 1000 caracteres")
    private String url;

    private String referenciaBibliografica;

    @io.swagger.v3.oas.annotations.media.Schema(hidden = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @AssertTrue(message = "Si el tipo de fuente es SITIO_WEB, la url es obligatoria "
            + "y debe tener un formato válido (http:// o https://)")
    public boolean isUrlValidaParaSitioWeb() {
        if (tipoFuente != TipoFuente.SITIO_WEB) {
            return true;
        }
        return url != null && URL_VALIDA.matcher(url.trim()).matches();
    }

    @io.swagger.v3.oas.annotations.media.Schema(hidden = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @AssertTrue(message = "El año debe estar entre 1000 y el año actual")
    public boolean isAnioValido() {
        if (anio == null) {
            return true;
        }
        int actual = Year.now().getValue();
        return anio >= ANIO_MINIMO && anio <= actual;
    }
}