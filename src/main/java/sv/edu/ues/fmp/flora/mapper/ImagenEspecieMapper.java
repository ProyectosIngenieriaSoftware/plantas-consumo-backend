package sv.edu.ues.fmp.flora.mapper;

import org.springframework.stereotype.Component;
import sv.edu.ues.fmp.flora.dto.request.ImagenEspecieArchivoRequest;
import sv.edu.ues.fmp.flora.dto.request.ImagenEspecieRequest;
import sv.edu.ues.fmp.flora.dto.response.ImagenEspecieResponse;
import sv.edu.ues.fmp.flora.entity.*;
import sv.edu.ues.fmp.flora.entity.enums.OrigenImagen;
import sv.edu.ues.fmp.flora.util.FormatoDeImagen;

/** Convierte imágenes y DTOs; recibe las relaciones resueltas, sin consultar repositorios. */
@Component
public class ImagenEspecieMapper {
    /**
     * Construye una imagen de origen URL con las relaciones previamente
     * validadas. El origen se asigna aquí y no se toma del request: el cliente
     * no elige el origen, lo elige la ruta por la que entró.
     */
    public ImagenEspecie toEntity(ImagenEspecieRequest request, Especie especie,
                                  EspecieParteComestible parte) {
        ImagenEspecie entidad = ImagenEspecie.builder()
                .especie(especie)
                .origen(OrigenImagen.URL)
                .build();
        updateEntity(entidad, request, parte);
        return entidad;
    }

    /**
     * Construye una imagen de origen PROPIA: los metadatos los trae el
     * formulario y los del archivo los pone quien ya lo validó.
     * <p>
     * {@code urlImagen} se queda en null, que es lo que exige
     * {@code ck_imagen_fuente_unica} para una fila PROPIA. El tipo de contenido
     * sale del formato <em>detectado</em>, nunca del que declaró el cliente.
     *
     * @param nombreArchivo  el nombre generado por el sistema (UUID + extensión)
     * @param formato        el formato detectado por los magic bytes
     * @param tamanioBytes   lo que pesa el archivo
     * @param nombreOriginal como se llamaba en la máquina de quien lo subió;
     *                       informativo
     */
    public ImagenEspecie toEntityArchivo(ImagenEspecieArchivoRequest request, Especie especie,
                                         EspecieParteComestible parte, String nombreArchivo,
                                         FormatoDeImagen formato, long tamanioBytes,
                                         String nombreOriginal) {
        return ImagenEspecie.builder()
                .especie(especie)
                .especieParte(parte)
                .origen(OrigenImagen.PROPIA)
                .urlImagen(null)
                .rutaArchivo(nombreArchivo)
                .nombreOriginal(limpiarTexto(nombreOriginal))
                .tipoContenido(formato.tipoContenido())
                .tamanioBytes(tamanioBytes)
                .tipoImagen(request.getTipoImagen())
                .titulo(limpiarTexto(request.getTitulo()))
                .descripcion(limpiarTexto(request.getDescripcion()))
                .textoAlternativo(limpiarTexto(request.getTextoAlternativo()))
                .creditos(limpiarTexto(request.getCreditos()))
                .esPrincipal(Boolean.TRUE.equals(request.getEsPrincipal()))
                .activa(true)
                .build();
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
                .origen(entidad.getOrigen())
                .urlImagen(entidad.getUrlImagen())
                .nombreOriginal(entidad.getNombreOriginal())
                .tipoContenido(entidad.getTipoContenido())
                .tamanioBytes(entidad.getTamanioBytes())
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
