package sv.edu.ues.fmp.flora.service;

import java.util.List;

import sv.edu.ues.fmp.flora.dto.request.VideoEspecieRequest;
import sv.edu.ues.fmp.flora.dto.response.VideoEspecieResponse;

public interface VideoEspecieService {

    List<VideoEspecieResponse> listarPorEspecie(Long idEspecie, boolean soloActivos);

    VideoEspecieResponse obtenerPorId(Long id);

    VideoEspecieResponse crear(Long idEspecie, VideoEspecieRequest request);

    VideoEspecieResponse actualizar(Long id, VideoEspecieRequest request);

    void desactivar(Long id);

    VideoEspecieResponse activar(Long id);
}
