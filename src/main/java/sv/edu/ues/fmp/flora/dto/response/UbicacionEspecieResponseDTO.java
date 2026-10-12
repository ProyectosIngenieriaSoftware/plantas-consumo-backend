package sv.edu.ues.fmp.flora.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UbicacionEspecieResponseDTO {
    private Long idUbicacion;
    private Long idEspecie;
    private Long idDistrito;
    private String nombreDistrito;
    private String departamentoDistrito;
    private String nombreLugar;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String descripcion;
    private LocalDate fechaObservacion;
    private Boolean activa;
    private LocalDateTime fechaRegistro;
}