package sv.edu.ues.fmp.flora.service;

import sv.edu.ues.fmp.flora.dto.request.UbicacionEspecieRequestDTO;
import sv.edu.ues.fmp.flora.dto.response.UbicacionEspecieResponseDTO;

import java.util.List;

public interface UbicacionEspecieService {

    UbicacionEspecieResponseDTO registrarUbicacion(UbicacionEspecieRequestDTO dto);

    List<UbicacionEspecieResponseDTO> obtenerUbicacionesPorEspecie(Long idEspecie);

    UbicacionEspecieResponseDTO actualizarUbicacion(Long idUbicacion, UbicacionEspecieRequestDTO dto);

    void desactivarUbicacion(Long idUbicacion);
}