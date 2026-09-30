package sv.edu.ues.fmp.flora.service;

import java.util.List;
import sv.edu.ues.fmp.flora.dto.request.ParteBeneficioRequest;
import sv.edu.ues.fmp.flora.dto.request.ParteBeneficioUpdateRequest;
import sv.edu.ues.fmp.flora.dto.response.ParteBeneficioResponse;

public interface ParteBeneficioService {
    List<ParteBeneficioResponse> listarTodas();
    List<ParteBeneficioResponse> listarActivas();
    ParteBeneficioResponse obtenerPorId(Long idEspecieParte, Long idBeneficio);
    ParteBeneficioResponse crear(ParteBeneficioRequest request);
    ParteBeneficioResponse actualizar(Long idEspecieParte, Long idBeneficio, ParteBeneficioUpdateRequest request);
    void desactivar(Long idEspecieParte, Long idBeneficio);
}
