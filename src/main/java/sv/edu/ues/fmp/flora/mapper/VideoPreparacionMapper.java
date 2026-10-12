package sv.edu.ues.fmp.flora.mapper;

import org.springframework.stereotype.Component;

import sv.edu.ues.fmp.flora.dto.request.VideoPreparacionRequest;
import sv.edu.ues.fmp.flora.dto.response.VideoPreparacionResponse;
import sv.edu.ues.fmp.flora.entity.PreparacionConsumo;
import sv.edu.ues.fmp.flora.entity.VideoPreparacion;

/** Convierte entre VideoPreparacion y sus correspondientes DTOs. */
@Component
public class VideoPreparacionMapper {

    /** Construye una nueva entidad VideoPreparacion a partir del request y la preparacion. */
    public VideoPreparacion toEntity(VideoPreparacionRequest request, PreparacionConsumo preparacion) {
        VideoPreparacion entidad = VideoPreparacion.builder()
                .preparacion(preparacion)
                .build();
        updateEntity(entidad, request);
        return entidad;
    }

    /** Actualiza los campos editables de la entidad VideoPreparacion. */
    public void updateEntity(VideoPreparacion entidad, VideoPreparacionRequest request) {
        entidad.setTitulo(limpiarTexto(request.getTitulo()));
        entidad.setDescripcion(limpiarTexto(request.getDescripcion()));
        entidad.setUrlVideo(limpiarTexto(request.getUrlVideo()));
        entidad.setDuracionSegundos(request.getDuracionSegundos());
        entidad.setCreditos(limpiarTexto(request.getCreditos()));
    }

    /** Convierte la entidad a su DTO de respuesta. */
    public VideoPreparacionResponse toResponse(VideoPreparacion entidad) {
        return VideoPreparacionResponse.builder()
                .idVideoPreparacion(entidad.getIdVideoPreparacion())
                .idPreparacion(entidad.getPreparacion() != null
                        ? entidad.getPreparacion().getIdPreparacion()
                        : null)
                .titulo(entidad.getTitulo())
                .descripcion(entidad.getDescripcion())
                .urlVideo(entidad.getUrlVideo())
                .duracionSegundos(entidad.getDuracionSegundos())
                .creditos(entidad.getCreditos())
                .activo(entidad.getActivo())
                .fechaRegistro(entidad.getFechaRegistro())
                .build();
    }

    private String limpiarTexto(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.strip();
        return limpio.isEmpty() ? null : limpio;
    }
}

