package sv.edu.ues.fmp.flora.service;

import java.util.List;

import sv.edu.ues.fmp.flora.dto.request.PartePlantaRequest;
import sv.edu.ues.fmp.flora.dto.response.PartePlantaResponse;

/**
 * Contrato de negocio para las partes de planta.
 * Trabaja solo con DTOs: la entidad no cruza hacia el controlador.
 */

//contrato
public interface PartePlantaService {

    List<PartePlantaResponse> listarTodas();

    List<PartePlantaResponse> listarActivas();

    PartePlantaResponse obtenerPorId(Long id);

    PartePlantaResponse crear(PartePlantaRequest request);

    PartePlantaResponse actualizar(Long id, PartePlantaRequest request);

    void desactivar(Long id);



}
