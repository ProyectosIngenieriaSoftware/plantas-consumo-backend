package sv.edu.ues.fmp.flora.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sv.edu.ues.fmp.flora.entity.enums.TipoImagen;

/**
 * Los metadatos que acompanan a una imagen subida como archivo.
 *
 * <h2>Por que no se reusa {@link ImagenEspecieRequest}</h2>
 * Ese DTO exige {@code urlImagen} ({@code @NotBlank} mas el patron
 * {@code http(s)://}), que es justo lo que una imagen propia no tiene. Reusarlo
 * pedia relajar la URL a opcional, y entonces el POST de siempre --el de
 * registrar un enlace-- dejaria de exigirla: se podria crear una imagen URL sin
 * URL y el 400 se convertiria en el 409 del CHECK de la base, mas tarde y con
 * peor mensaje. Son dos entradas distintas y cada una valida lo suyo.
 * <p>
 * El resto de los campos son los mismos y con las mismas reglas, para que una
 * imagen propia y un enlace se describan igual en el catalogo.
 *
 * <h2>Como llega</h2>
 * En el mismo {@code multipart/form-data} que el archivo, como campos del
 * formulario. Spring los enlaza con {@code @ModelAttribute}, de modo que las
 * anotaciones de validacion funcionan igual que en un cuerpo JSON. El archivo
 * viaja aparte, en su propia parte.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImagenEspecieArchivoRequest {

    /** Opcional; si viene, la parte tiene que ser de la misma especie. */
    private Long idEspecieParte;

    @NotNull(message = "El tipo de imagen es obligatorio")
    private TipoImagen tipoImagen;

    @Size(max = 200, message = "El título no puede exceder 200 caracteres")
    private String titulo;

    private String descripcion;

    /**
     * Obligatorio igual que al registrar un enlace: es el texto que lee quien
     * no puede ver la imagen, y una imagen del catalogo sin el es una imagen
     * inaccesible.
     * <p>
     * Es {@code @NotNull} y no {@code @NotBlank} porque en un formulario
     * multipart un campo que no se envia llega como null y uno que se envia
     * vacio llega como cadena vacia; {@link #isTextoAlternativoPresente()}
     * cubre los dos casos con un mensaje unico.
     */
    @Size(max = 300, message = "El texto alternativo no puede exceder 300 caracteres")
    private String textoAlternativo;

    @Size(max = 250, message = "Los créditos no pueden exceder 250 caracteres")
    private String creditos;

    /** Omitido o nulo equivale a false, igual que al registrar un enlace. */
    private Boolean esPrincipal;

    @io.swagger.v3.oas.annotations.media.Schema(hidden = true)
    @jakarta.validation.constraints.AssertTrue(
            message = "El texto alternativo es obligatorio")
    public boolean isTextoAlternativoPresente() {
        return textoAlternativo != null && !textoAlternativo.isBlank();
    }
}
