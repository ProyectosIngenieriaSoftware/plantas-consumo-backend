package sv.edu.ues.fmp.flora.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos que salen de una parte comestible de una especie.
 * <p>
 * Incluye el nombre cientifico de la especie y el nombre de la parte para que
 * el cliente no tenga que resolver los ids con otra peticion, en especial al
 * consultar por la ruta plana {@code /api/partes-comestibles/{id}}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecieParteComestibleResponse {

    private Long idEspecieParte;
    private Long idEspecie;
    private String nombreCientificoEspecie;
    private Long idPartePlanta;
    private String nombreParte;
    private String descripcion;
    private String advertencias;
    private Boolean activa;
}
