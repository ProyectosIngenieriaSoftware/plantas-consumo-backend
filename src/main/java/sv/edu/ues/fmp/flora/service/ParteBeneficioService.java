package sv.edu.ues.fmp.flora.service;

import java.util.List;
import sv.edu.ues.fmp.flora.dto.request.ParteBeneficioRequest;
import sv.edu.ues.fmp.flora.dto.request.ParteBeneficioUpdateRequest;
import sv.edu.ues.fmp.flora.dto.response.ParteBeneficioResponse;

public interface ParteBeneficioService {
    List<ParteBeneficioResponse> listarTodas();
    List<ParteBeneficioResponse> listarActivas();

    /**
     * Los beneficios activos de una parte comestible concreta.
     * <p>
     * El listado por padre del modulo, el que le faltaba a la ficha publica. Al
     * contrario que los dos de arriba, no recorre todas las especies: entra por
     * el id de la parte, asi que el servicio puede preguntar por el estado de
     * la ficha de la que cuelga, igual que sus hermanos
     * {@code PreparacionConsumoService.listarPorParte} y
     * {@code EpocaCosechaService.listarPorParte}.
     */
    List<ParteBeneficioResponse> listarPorParte(Long idEspecieParte);

    ParteBeneficioResponse obtenerPorId(Long idEspecieParte, Long idBeneficio);
    ParteBeneficioResponse crear(ParteBeneficioRequest request);
    ParteBeneficioResponse actualizar(Long idEspecieParte, Long idBeneficio, ParteBeneficioUpdateRequest request);
    void desactivar(Long idEspecieParte, Long idBeneficio);
}
