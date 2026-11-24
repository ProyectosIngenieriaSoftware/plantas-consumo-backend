package sv.edu.ues.fmp.flora.mapper;

import org.springframework.stereotype.Component;

import sv.edu.ues.fmp.flora.dto.request.EspecieParteComestibleActualizarRequest;
import sv.edu.ues.fmp.flora.dto.request.EspecieParteComestibleRequest;
import sv.edu.ues.fmp.flora.dto.response.EspecieParteComestibleResponse;
import sv.edu.ues.fmp.flora.entity.Especie;
import sv.edu.ues.fmp.flora.entity.EspecieParteComestible;
import sv.edu.ues.fmp.flora.entity.PartePlanta;

/**
 * Convierte entre los DTOs de parte comestible y la entidad.
 * No consulta repositorios: la especie y la parte llegan ya resueltas y
 * validadas por el servicio.
 */
@Component
public class EspecieParteComestibleMapper {

    public EspecieParteComestible toEntity(EspecieParteComestibleRequest request,
                                           Especie especie, PartePlanta partePlanta) {
        return EspecieParteComestible.builder()
                .especie(especie)
                .partePlanta(partePlanta)
                .descripcion(limpiarTextoLargo(request.getDescripcion()))
                .advertencias(limpiarTextoLargo(request.getAdvertencias()))
                .build();
    }

    /**
     * Copia los campos editables sobre una entidad ya gestionada. No toca la
     * especie, la parte (inmutable por regla de negocio) ni {@code activa}
     * (tiene endpoints propios).
     */
    public void updateEntity(EspecieParteComestible entity,
                             EspecieParteComestibleActualizarRequest request) {
        entity.setDescripcion(limpiarTextoLargo(request.getDescripcion()));
        entity.setAdvertencias(limpiarTextoLargo(request.getAdvertencias()));
    }

    public EspecieParteComestibleResponse toResponse(EspecieParteComestible entity) {
        return EspecieParteComestibleResponse.builder()
                .idEspecieParte(entity.getIdEspecieParte())
                .idEspecie(entity.getEspecie().getIdEspecie())
                .nombreCientificoEspecie(entity.getEspecie().getNombreCientifico())
                .idPartePlanta(entity.getPartePlanta().getIdPartePlanta())
                .nombreParte(entity.getPartePlanta().getNombre())
                .descripcion(entity.getDescripcion())
                .advertencias(entity.getAdvertencias())
                .activa(entity.getActiva())
                .build();
    }

    /**
     * Limpieza minima para textos largos: quita espacios en los extremos y
     * convierte la cadena vacia o en blanco en null.
     * <p>
     * No usa {@code Textos.normalizar} a proposito: esa utilidad colapsa todo
     * espacio interno, saltos de linea incluidos, y destruiria el formato de
     * parrafos o listas de advertencias.
     */
    private String limpiarTextoLargo(String valor) {
        if (valor == null) return null;
        String limpio = valor.strip();
        return limpio.isEmpty() ? null : limpio;
    }
}
