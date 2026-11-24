package sv.edu.ues.fmp.flora.mapper;

import org.springframework.stereotype.Component;
import sv.edu.ues.fmp.flora.dto.request.EspecieHabitatRequest;
import sv.edu.ues.fmp.flora.dto.response.EspecieHabitatResponse;
import sv.edu.ues.fmp.flora.entity.Especie;
import sv.edu.ues.fmp.flora.entity.EspecieHabitat;
import sv.edu.ues.fmp.flora.entity.Habitat;
import sv.edu.ues.fmp.flora.entity.EspecieHabitatId;
import sv.edu.ues.fmp.flora.dto.request.EspecieHabitatActualizarRequest;

/** Convierte solicitudes y respuestas de la relación sin acceder a repositorios. */
@Component
public class EspecieHabitatMapper {

    /**
     * Construye la relación y su clave compuesta con los padres que el servicio
     * ya recuperó y validó. Normaliza los extremos del texto sin consultar la base de datos.
     */
    public EspecieHabitat toEntity(EspecieHabitatRequest request, Especie especie, Habitat habitat) {
        return EspecieHabitat.builder()
                .id(new EspecieHabitatId(especie.getIdEspecie(), habitat.getIdHabitat()))
                .especie(especie)
                .habitat(habitat)
                .observacion(normalizarObservacion(request.getObservacion()))
                .build();
    }

    /**
     * Copia únicamente la observación, incluido null si se desea quitarla.
     * Conserva la clave compuesta y las referencias a los padres.
     */
    public void updateEntity(EspecieHabitat entity, EspecieHabitatActualizarRequest request) {
        entity.setObservacion(normalizarObservacion(request.getObservacion()));
    }

    /**
     * Convierte la relación en un DTO con ambos IDs, los nombres de los padres
     * y la observación. Debe ejecutarse con las asociaciones disponibles, como
     * ocurre dentro de las transacciones del servicio.
     */
    public EspecieHabitatResponse toResponse(EspecieHabitat entity) {
        return EspecieHabitatResponse.builder()
                .idEspecie(entity.getId().getIdEspecie())
                .idHabitat(entity.getId().getIdHabitat())
                .nombreCientificoEspecie(entity.getEspecie().getNombreCientifico())
                .nombreHabitat(entity.getHabitat().getNombre())
                .observacion(entity.getObservacion())
                .build();
    }

    /** Quita espacios exteriores y convierte texto vacío a null; conserva los saltos internos. */
    private String normalizarObservacion(String observacion) {
        if (observacion == null) {
            return null;
        }
        String texto = observacion.strip();
        return texto.isEmpty() ? null : texto;
    }
}
