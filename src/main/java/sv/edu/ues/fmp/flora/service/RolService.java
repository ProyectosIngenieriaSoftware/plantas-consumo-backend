package sv.edu.ues.fmp.flora.service;

import sv.edu.ues.fmp.flora.dto.request.RolRequest;
import sv.edu.ues.fmp.flora.dto.response.PaginaResponse;
import sv.edu.ues.fmp.flora.dto.response.RolResponse;

public interface RolService {

    PaginaResponse<RolResponse> listarTodos(int pagina, int tamanio);

    PaginaResponse<RolResponse> listarActivos(int pagina, int tamanio);

    RolResponse obtenerPorId(Long id);

    RolResponse crear(RolRequest request);

    RolResponse actualizar(Long id, RolRequest request);

    void desactivar(Long id);

    RolResponse reactivar(Long id);
}
