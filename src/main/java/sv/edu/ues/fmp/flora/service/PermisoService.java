package sv.edu.ues.fmp.flora.service;

import sv.edu.ues.fmp.flora.dto.request.PermisoRequest;
import sv.edu.ues.fmp.flora.dto.response.PaginaResponse;
import sv.edu.ues.fmp.flora.dto.response.PermisoResponse;

public interface PermisoService {

    PaginaResponse<PermisoResponse> listarTodos(int pagina, int tamanio);

    PaginaResponse<PermisoResponse> listarActivos(int pagina, int tamanio);

    PermisoResponse obtenerPorId(Long id);

    PermisoResponse crear(PermisoRequest request);

    PermisoResponse actualizar(Long id, PermisoRequest request);

    void desactivar(Long id);

    PermisoResponse reactivar(Long id);
}
