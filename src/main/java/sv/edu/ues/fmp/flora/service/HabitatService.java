package sv.edu.ues.fmp.flora.service;

import java.util.List;

import sv.edu.ues.fmp.flora.dto.request.HabitatRequest;
import sv.edu.ues.fmp.flora.dto.response.HabitatResponse;

public interface HabitatService {

    List<HabitatResponse> listarTodos();

    List<HabitatResponse> listarActivos();

    List<HabitatResponse> buscarPorNombre(String nombre);

    HabitatResponse obtenerPorId(Long id);

    HabitatResponse crear(HabitatRequest request);

    HabitatResponse actualizar(Long id, HabitatRequest request);

    void desactivar(Long id);
}