package sv.edu.ues.fmp.flora.service;

import java.util.List;
import sv.edu.ues.fmp.flora.dto.request.BeneficioRequest;
import sv.edu.ues.fmp.flora.dto.response.BeneficioResponse;

public interface BeneficioService {

    List<BeneficioResponse> listarTodos();

    /**
     * El catalogo sin los beneficios dados de baja.
     * <p>
     * Lo pide {@code GET /api/beneficios?soloActivos=true}. Es el listado que
     * le sirve a un selector: desde que el DELETE da de baja en lugar de
     * borrar, {@link #listarTodos()} sigue devolviendo los de baja --y asi
     * tiene que ser, para que la pantalla de administracion pueda verlos y
     * reactivarlos-- pero un formulario no deberia ofrecerlos.
     */
    List<BeneficioResponse> listarActivos();

    BeneficioResponse obtenerPorId(Long id);

    BeneficioResponse crear(BeneficioRequest request);

    BeneficioResponse actualizar(Long id, BeneficioRequest request);

    /**
     * Baja logica: pone {@code activo = false} y deja la fila donde esta.
     * <p>
     * Antes era un borrado fisico, y por eso un beneficio ya usado en
     * {@code parte_beneficio} no se podia borrar: la FK saltaba y el
     * {@code GlobalExceptionHandler} la traducia a un 409 de mensaje generico
     * ("La operacion viola una restriccion de la base de datos"), que no le
     * decia a nadie que el problema era que el beneficio estaba en uso. Con la
     * baja logica ese caso responde 204 como cualquier otro y las fichas que
     * lo citan conservan su historia.
     * <p>
     * Es el patron del resto del catalogo: {@code HabitatService.desactivar},
     * {@code PartePlantaService.desactivar}. Se revierte con
     * {@link #activar(Long)}.
     */
    void desactivar(Long id);

    /**
     * Revierte la baja logica: {@code activo = true}.
     * <p>
     * Idempotente, como sus hermanos: sobre un beneficio que ya esta activo no
     * cambia nada y devuelve el registro igualmente.
     */
    BeneficioResponse activar(Long id);
}