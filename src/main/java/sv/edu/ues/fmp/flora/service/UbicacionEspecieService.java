package sv.edu.ues.fmp.flora.service;

import sv.edu.ues.fmp.flora.dto.request.UbicacionEspecieRequest;
import sv.edu.ues.fmp.flora.dto.response.UbicacionEspecieResponse;

import java.util.List;

public interface UbicacionEspecieService {

    UbicacionEspecieResponse crear(UbicacionEspecieRequest request);

    List<UbicacionEspecieResponse> obtenerUbicacionesPorEspecie(Long idEspecie);

    UbicacionEspecieResponse actualizarUbicacion(Long idUbicacion, UbicacionEspecieRequest request);

    void desactivarUbicacion(Long idUbicacion);
}