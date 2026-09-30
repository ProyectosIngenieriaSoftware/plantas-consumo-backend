package sv.edu.ues.fmp.flora.service;

import java.util.List;
import sv.edu.ues.fmp.flora.dto.request.EspecieHabitatActualizarRequest;
import sv.edu.ues.fmp.flora.dto.request.EspecieHabitatRequest;
import sv.edu.ues.fmp.flora.dto.response.EspecieHabitatResponse;

/** Operaciones para consultar y gestionar las asociaciones de especies y hábitats. */
public interface EspecieHabitatService {
    /**
     * Obtiene todas las relaciones como DTOs de respuesta.
     */
    List<EspecieHabitatResponse> listarTodos();
    /**
     * Obtiene una relación por su clave compuesta; falla si el par no existe.
     */
    EspecieHabitatResponse obtenerPorId(Long idEspecie, Long idHabitat);
    /**
     * Consulta las relaciones de una especie existente.
     * Devuelve una lista vacía si la especie aún no tiene hábitats asociados.
     */
    List<EspecieHabitatResponse> listarPorEspecie(Long idEspecie);
    /**
     * Consulta las relaciones de un hábitat existente.
     * Devuelve una lista vacía si el hábitat aún no tiene especies asociadas.
     */
    List<EspecieHabitatResponse> listarPorHabitat(Long idHabitat);
    /**
     * Crea una relación después de validar los IDs, la existencia de ambos padres
     * y la ausencia de la misma combinación.
     */
    EspecieHabitatResponse crear(Long idEspecie, EspecieHabitatRequest request);
    /**
     * Actualiza únicamente la observación de una relación existente.
     * La clave se toma de la URL y no puede modificarse.
     */
    EspecieHabitatResponse actualizar(Long idEspecie, Long idHabitat, EspecieHabitatActualizarRequest request);
    /**
     * Elimina únicamente la relación; falla si la combinación no existe.
     */
    void eliminar(Long idEspecie, Long idHabitat);
}
