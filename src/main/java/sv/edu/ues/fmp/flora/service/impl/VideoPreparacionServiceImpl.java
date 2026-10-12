package sv.edu.ues.fmp.flora.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.dto.request.VideoPreparacionRequest;
import sv.edu.ues.fmp.flora.dto.response.VideoPreparacionResponse;
import sv.edu.ues.fmp.flora.entity.PreparacionConsumo;
import sv.edu.ues.fmp.flora.entity.VideoPreparacion;
import sv.edu.ues.fmp.flora.exception.EstadoInvalidoException;
import sv.edu.ues.fmp.flora.exception.RecursoNoEncontradoException;
import sv.edu.ues.fmp.flora.mapper.VideoPreparacionMapper;
import sv.edu.ues.fmp.flora.repository.PreparacionConsumoRepository;
import sv.edu.ues.fmp.flora.repository.VideoPreparacionRepository;
import sv.edu.ues.fmp.flora.service.VideoPreparacionService;

/** Implementacion del servicio para la gestion de videos de preparaciones de consumo. */
@Service
@RequiredArgsConstructor
@Transactional
public class VideoPreparacionServiceImpl implements VideoPreparacionService {

    private final VideoPreparacionRepository videoPreparacionRepository;
    private final PreparacionConsumoRepository preparacionConsumoRepository;
    private final VideoPreparacionMapper videoPreparacionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<VideoPreparacionResponse> listarPorPreparacion(Long idPreparacion, boolean soloActivos) {
        verificarPreparacionExisteOFallar(idPreparacion);
        List<VideoPreparacion> videos = soloActivos
                ? videoPreparacionRepository.findByPreparacionIdPreparacionAndActivoTrueOrderByIdVideoPreparacionAsc(idPreparacion)
                : videoPreparacionRepository.findByPreparacionIdPreparacionOrderByIdVideoPreparacionAsc(idPreparacion);
        return videos.stream().map(videoPreparacionMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VideoPreparacionResponse obtenerPorId(Long id) {
        return videoPreparacionMapper.toResponse(buscarOFallar(id));
    }

    @Override
    public VideoPreparacionResponse crear(Long idPreparacion, VideoPreparacionRequest request) {
        PreparacionConsumo preparacion = buscarPreparacionOFallar(idPreparacion);
        verificarPreparacionActiva(preparacion);

        VideoPreparacion nuevo = videoPreparacionMapper.toEntity(request, preparacion);
        return videoPreparacionMapper.toResponse(videoPreparacionRepository.save(nuevo));
    }

    @Override
    public VideoPreparacionResponse actualizar(Long id, VideoPreparacionRequest request) {
        VideoPreparacion video = buscarOFallar(id);
        verificarPreparacionActiva(video.getPreparacion());

        videoPreparacionMapper.updateEntity(video, request);
        return videoPreparacionMapper.toResponse(video);
    }

    @Override
    public void desactivar(Long id) {
        VideoPreparacion video = buscarOFallar(id);
        if (!Boolean.TRUE.equals(video.getActivo())) {
            return;
        }
        video.setActivo(false);
    }

    @Override
    public VideoPreparacionResponse activar(Long id) {
        VideoPreparacion video = buscarOFallar(id);
        if (Boolean.TRUE.equals(video.getActivo())) {
            return videoPreparacionMapper.toResponse(video);
        }

        verificarPreparacionActiva(video.getPreparacion());
        video.setActivo(true);
        return videoPreparacionMapper.toResponse(video);
    }

    private VideoPreparacion buscarOFallar(Long id) {
        return videoPreparacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el video de preparación con id " + id));
    }

    private PreparacionConsumo buscarPreparacionOFallar(Long idPreparacion) {
        return preparacionConsumoRepository.findById(idPreparacion)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la preparación de consumo con id " + idPreparacion));
    }

    private void verificarPreparacionExisteOFallar(Long idPreparacion) {
        if (!preparacionConsumoRepository.existsById(idPreparacion)) {
            throw new RecursoNoEncontradoException(
                    "No existe la preparación de consumo con id " + idPreparacion);
        }
    }

    private void verificarPreparacionActiva(PreparacionConsumo preparacion) {
        if (!Boolean.TRUE.equals(preparacion.getActiva())) {
            throw new EstadoInvalidoException(
                    "La preparación '" + preparacion.getNombre()
                            + "' está desactivada; no se pueden gestionar sus videos.");
        }
    }
}

