package sv.edu.ues.fmp.flora.service;

import java.util.List;

import sv.edu.ues.fmp.flora.dto.request.EspecieRequest;
import sv.edu.ues.fmp.flora.dto.request.TaxonomiaRequest;
import sv.edu.ues.fmp.flora.dto.response.EspecieResponse;
import sv.edu.ues.fmp.flora.entity.enums.EstadoPublicacion;

/**
 * Contrato de negocio de las especies, incluido su flujo editorial.
 * Trabaja solo con DTOs: la entidad no cruza hacia el controlador.
 * <p>
 * El flujo de publicacion es: BORRADOR -> EN_REVISION -> (validar) ->
 * PUBLICADA, con salida a RECHAZADA desde EN_REVISION y vuelta a EN_REVISION
 * desde RECHAZADA.
 */
public interface EspecieService {

    List<EspecieResponse> listarTodas();

    /** Catalogo publico: solo fichas PUBLICADA y activas. */
    List<EspecieResponse> listarPublicadas();

    List<EspecieResponse> listarPorEstado(EstadoPublicacion estado);

    EspecieResponse obtenerPorId(Long id);

    /** Crea la especie y su taxonomia en la misma transaccion. */
    EspecieResponse crear(EspecieRequest request);

    /** Actualiza los campos editables de la ficha y su taxonomia. */
    EspecieResponse actualizar(Long id, EspecieRequest request);

    /**
     * Rectifica solo la clasificacion taxonomica, sin tocar el resto de la
     * ficha. Evita tener que reenviar la especie completa para corregir un
     * genero o una familia.
     */
    EspecieResponse actualizarTaxonomia(Long idEspecie, TaxonomiaRequest request);

    EspecieResponse enviarARevision(Long id);

    /** Marca la ficha como revisada sin publicarla todavia. */
    EspecieResponse validar(Long id, Long idValidador);

    EspecieResponse publicar(Long id, Long idPublicador);

    EspecieResponse rechazar(Long id);

    /** Baja logica. Nunca DELETE fisico: varias tablas referencian la especie. */
    void desactivar(Long id);
}
