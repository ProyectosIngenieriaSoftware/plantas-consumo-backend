package sv.edu.ues.fmp.flora.mapper;

import org.springframework.stereotype.Component;

import sv.edu.ues.fmp.flora.dto.request.ImagenPreparacionRequest;
import sv.edu.ues.fmp.flora.dto.response.ImagenPreparacionResponse;
import sv.edu.ues.fmp.flora.entity.ImagenPreparacion;
import sv.edu.ues.fmp.flora.entity.PreparacionConsumo;

/** Convierte entre ImagenPreparacion y sus correspondientes DTOs. */
@Component
public class ImagenPreparacionMapper {


    public ImagenPreparacion toEntity(ImagenPreparacionRequest request, PreparacionConsumo preparacion) {
        ImagenPreparacion entidad = ImagenPreparacion.builder()
                .preparacion(preparacion)
                .build();
        updateEntity(entidad, request);
        return entidad;
    }


    public void updateEntity(ImagenPreparacion entidad, ImagenPreparacionRequest request) {
        entidad.setUrlImagen(limpiarTexto(request.getUrlImagen()));
        entidad.setTitulo(limpiarTexto(request.getTitulo()));
        entidad.setDescripcion(limpiarTexto(request.getDescripcion()));
        entidad.setTextoAlternativo(limpiarTexto(request.getTextoAlternativo()));
        entidad.setCreditos(limpiarTexto(request.getCreditos()));
        entidad.setEsPrincipal(Boolean.TRUE.equals(request.getEsPrincipal()));
    }


    public ImagenPreparacionResponse toResponse(ImagenPreparacion entidad) {
        return ImagenPreparacionResponse.builder()
                .idImagenPreparacion(entidad.getIdImagenPreparacion())
                .idPreparacion(entidad.getPreparacion() != null
                        ? entidad.getPreparacion().getIdPreparacion()
                        : null)
                .urlImagen(entidad.getUrlImagen())
                .titulo(entidad.getTitulo())
                .descripcion(entidad.getDescripcion())
                .textoAlternativo(entidad.getTextoAlternativo())
                .creditos(entidad.getCreditos())
                .esPrincipal(entidad.getEsPrincipal())
                .activa(entidad.getActiva())
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

