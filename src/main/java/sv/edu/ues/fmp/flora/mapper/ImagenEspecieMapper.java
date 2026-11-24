package sv.edu.ues.fmp.flora.mapper;

import org.springframework.stereotype.Component;
import sv.edu.ues.fmp.flora.dto.request.ImagenEspecieRequest;
import sv.edu.ues.fmp.flora.dto.response.ImagenEspecieResponse;
import sv.edu.ues.fmp.flora.entity.*;

/** Convierte imágenes y DTOs; recibe las relaciones resueltas, sin consultar repositorios. */
@Component
public class ImagenEspecieMapper {
    /** Construye una imagen activa con las relaciones previamente validadas. */
    public ImagenEspecie toEntity(ImagenEspecieRequest request, Especie especie,
                                  EspecieParteComestible parte) {
        ImagenEspecie entidad = ImagenEspecie.builder().especie(especie).build();
        updateEntity(entidad, request, parte);
        return entidad;
    }

    /** Reemplaza campos editables sin cambiar especie, estado ni fecha de registro. */
    public void updateEntity(ImagenEspecie entidad, ImagenEspecieRequest request,
                             EspecieParteComestible parte) {
        entidad.setEspecieParte(parte);
        entidad.setTipoImagen(request.getTipoImagen());
        entidad.setUrlImagen(limpiarTexto(request.getUrlImagen()));
        entidad.setTitulo(limpiarTexto(request.getTitulo()));
        entidad.setDescripcion(limpiarTexto(request.getDescripcion()));
        entidad.setTextoAlternativo(limpiarTexto(request.getTextoAlternativo()));
        entidad.setCreditos(limpiarTexto(request.getCreditos()));
        entidad.setEsPrincipal(Boolean.TRUE.equals(request.getEsPrincipal()));
    }

    /** Obtiene los identificadores dentro de la transacción, evitando serializar entidades. */
    public ImagenEspecieResponse toResponse(ImagenEspecie entidad) {
        return ImagenEspecieResponse.builder()
                .idImagen(entidad.getIdImagen())
                .idEspecie(entidad.getEspecie().getIdEspecie())
                .idEspecieParte(entidad.getEspecieParte() == null ? null
                        : entidad.getEspecieParte().getIdEspecieParte())
                .tipoImagen(entidad.getTipoImagen())
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

    /** Conserva saltos de línea internos; no utiliza Textos.normalizar. */
    private String limpiarTexto(String valor) {
        if (valor == null) return null;
        String limpio = valor.strip();
        return limpio.isEmpty() ? null : limpio;
    }
}
