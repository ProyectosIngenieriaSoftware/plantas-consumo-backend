package sv.edu.ues.fmp.flora.service;

import java.util.List;

import sv.edu.ues.fmp.flora.dto.request.FuenteRequest;
import sv.edu.ues.fmp.flora.dto.response.FuenteResponse;


public interface FuenteService {

    List<FuenteResponse> listarTodas();

    List<FuenteResponse> listarActivas();

    FuenteResponse obtenerPorId(Long id);

    FuenteResponse crear(FuenteRequest request);

    FuenteResponse actualizar(Long id, FuenteRequest request);

    void desactivar(Long id);

}
