package sv.edu.ues.fmp.flora.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecieFuenteRequest {

    @NotNull(message = "El identificador de la fuente es obligatorio")
    private Long idFuente;

    @Size(max = 2000, message = "La observación no puede exceder 2000 caracteres")
    private String observacion;
}