package sv.edu.ues.fmp.flora.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;


@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MunicipioResponse {
    private Long idMunicipio;
    private String nombre;
    private Long idDepartamento;
    private String nombreDepartamento;
    private Boolean activo;
}