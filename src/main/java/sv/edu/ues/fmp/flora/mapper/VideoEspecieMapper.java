package sv.edu.ues.fmp.flora.mapper;

import org.springframework.stereotype.Component;

import sv.edu.ues.fmp.flora.dto.request.VideoEspecieRequest;
import sv.edu.ues.fmp.flora.dto.response.VideoEspecieResponse;
import sv.edu.ues.fmp.flora.entity.Especie;
import sv.edu.ues.fmp.flora.entity.EspecieParteComestible;
import sv.edu.ues.fmp.flora.entity.VideoEspecie;

@Component
public class VideoEspecieMapper {

    public VideoEspecie toEntity(VideoEspecieRequest request, Especie especie,
            EspecieParteComestible especieParte) {
        return VideoEspecie.builder()
                .especie(especie)
                .idEspecieParte(especieParte == null ? null : especieParte.getIdEspecieParte())
                .especieParte(especieParte)
                .titulo(normalizar(request.getTitulo()))
                .descripcion(normalizar(request.getDescripcion()))
                .urlVideo(normalizar(request.getUrlVideo()))
                .duracionSegundos(request.getDuracionSegundos())
                .creditos(normalizar(request.getCreditos()))
                .activo(true)
                .build();
    }

    public void updateEntity(VideoEspecie entity, VideoEspecieRequest request,
            EspecieParteComestible especieParte) {
        entity.setEspecieParte(especieParte);
        entity.setIdEspecieParte(especieParte == null ? null : especieParte.getIdEspecieParte());
        entity.setTitulo(normalizar(request.getTitulo()));
        entity.setDescripcion(normalizar(request.getDescripcion()));
        entity.setUrlVideo(normalizar(request.getUrlVideo()));
        entity.setDuracionSegundos(request.getDuracionSegundos());
        entity.setCreditos(normalizar(request.getCreditos()));
    }

    public VideoEspecieResponse toResponse(VideoEspecie entity) {
        return VideoEspecieResponse.builder()
                .idVideo(entity.getIdVideo())
                .idEspecie(entity.getEspecie().getIdEspecie())
                .idEspecieParte(entity.getEspecieParte() == null
                        ? null : entity.getEspecieParte().getIdEspecieParte())
                .titulo(entity.getTitulo())
                .descripcion(entity.getDescripcion())
                .urlVideo(entity.getUrlVideo())
                .duracionSegundos(entity.getDuracionSegundos())
                .creditos(entity.getCreditos())
                .activo(entity.getActivo())
                .fechaRegistro(entity.getFechaRegistro())
                .build();
    }

    private String normalizar(String valor) {
        if (valor == null) return null;
        String limpio = valor.strip();
        return limpio.isEmpty() ? null : limpio;
    }
}
