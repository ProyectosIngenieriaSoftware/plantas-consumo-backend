package sv.edu.ues.fmp.flora.service;

import java.util.List;

import sv.edu.ues.fmp.flora.dto.request.EspecieParteComestibleActualizarRequest;
import sv.edu.ues.fmp.flora.dto.request.EspecieParteComestibleRequest;
import sv.edu.ues.fmp.flora.dto.response.EspecieParteComestibleResponse;

/**
 * Contrato de negocio para las partes comestibles de una especie.
 * Trabaja solo con DTOs: la entidad no cruza hacia el controlador.
 * <p>
 * El listado y la creacion reciben el id de la especie; el resto de
 * operaciones se identifican con el id del propio registro.
 */
public interface EspecieParteComestibleService {

    /** Con {@code soloActivas} en false devuelve tambien las desactivadas. */
    List<EspecieParteComestibleResponse> listarPorEspecie(Long idEspecie, boolean soloActivas);

    EspecieParteComestibleResponse obtenerPorId(Long id);

    EspecieParteComestibleResponse crear(Long idEspecie, EspecieParteComestibleRequest request);

    /** Solo edita descripcion y advertencias: la parte asociada es inmutable. */
    EspecieParteComestibleResponse actualizar(Long id, EspecieParteComestibleActualizarRequest request);

    /** Baja logica e idempotente. */
    void desactivar(Long id);

    EspecieParteComestibleResponse activar(Long id);
}
