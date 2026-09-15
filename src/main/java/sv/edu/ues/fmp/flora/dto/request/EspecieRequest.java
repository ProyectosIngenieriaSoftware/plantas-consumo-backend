package sv.edu.ues.fmp.flora.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos que entran al crear o actualizar una especie.
 * <p>
 * No incluye {@code idEspecie}, {@code estadoPublicacion}, {@code activa},
 * {@code validadaPor}, {@code publicadaPor} ni fecha alguna: todos esos los
 * controla el servidor a traves de las transiciones de estado del servicio.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecieRequest {

    @NotBlank(message = "El nombre científico es obligatorio")
    @Size(max = 250, message = "El nombre científico no puede exceder 250 caracteres")
    private String nombreCientifico;

    /** La columna es NOT NULL en la base, por eso {@code @NotBlank} y no opcional. */
    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    @Size(max = 250, message = "El origen no puede exceder 250 caracteres")
    private String origen;

    private String propiedades;

    private String importanciaCultural;

    private String advertencias;

    // TODO: eliminar cuando se implemente JWT; se tomará del token del usuario autenticado
    @NotNull(message = "El usuario creador es obligatorio")
    private Long creadaPor;

    /**
     * Los datos taxonomicos viajan anidados porque la relacion es 1:1
     * obligatoria en ambos sentidos.
     * El {@code @Valid} es imprescindible: sin el, las validaciones de
     * {@link TaxonomiaRequest} no se ejecutan.
     */
    @NotNull(message = "La taxonomía es obligatoria")
    @Valid
    private TaxonomiaRequest taxonomia;
}
