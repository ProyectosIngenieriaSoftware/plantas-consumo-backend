package sv.edu.ues.fmp.flora.service;

import java.util.List;

import sv.edu.ues.fmp.flora.dto.request.VideoPreparacionRequest;
import sv.edu.ues.fmp.flora.dto.response.VideoPreparacionResponse;


public interface VideoPreparacionService {

    List<VideoPreparacionResponse> listarPorPreparacion(Long idPreparacion, boolean soloActivos);

    VideoPreparacionResponse obtenerPorId(Long id);

    VideoPreparacionResponse crear(Long idPreparacion, VideoPreparacionRequest request);

    VideoPreparacionResponse actualizar(Long id, VideoPreparacionRequest request);

    void desactivar(Long id);

    VideoPreparacionResponse activar(Long id);
}

