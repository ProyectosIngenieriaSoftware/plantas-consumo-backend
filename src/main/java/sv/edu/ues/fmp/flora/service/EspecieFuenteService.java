package sv.edu.ues.fmp.flora.service;

import java.util.List;

import sv.edu.ues.fmp.flora.dto.request.EspecieFuenteRequest;
import sv.edu.ues.fmp.flora.dto.response.EspecieFuenteResponse;

public interface EspecieFuenteService {

    List<EspecieFuenteResponse> listarPorEspecie(Long idEspecie);

    EspecieFuenteResponse registrar(Long idEspecie, EspecieFuenteRequest request);

    EspecieFuenteResponse actualizar(Long idEspecie, Long idFuente, EspecieFuenteRequest request);

    void eliminar(Long idEspecie, Long idFuente);
}