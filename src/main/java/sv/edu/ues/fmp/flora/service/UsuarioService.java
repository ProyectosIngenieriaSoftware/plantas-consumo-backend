package sv.edu.ues.fmp.flora.service;

import sv.edu.ues.fmp.flora.dto.request.UsuarioCambioClaveRequest;
import sv.edu.ues.fmp.flora.dto.request.UsuarioCreationRequest;
import sv.edu.ues.fmp.flora.dto.request.UsuarioLoginRequest;
import sv.edu.ues.fmp.flora.dto.request.UsuarioUpdateRequest;
import sv.edu.ues.fmp.flora.dto.response.PaginaResponse;
import sv.edu.ues.fmp.flora.dto.response.UsuarioResponse;

public interface UsuarioService {

    PaginaResponse<UsuarioResponse> listarTodos(int pagina, int tamanio);

    PaginaResponse<UsuarioResponse> listarActivos(int pagina, int tamanio);

    UsuarioResponse obtenerPorId(Long id);

    UsuarioResponse crear(UsuarioCreationRequest request);

    UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request);

    void desactivar(Long id);

    UsuarioResponse reactivar(Long id);

    void cambiarClave(Long id, UsuarioCambioClaveRequest request);

    UsuarioResponse iniciarSesion(UsuarioLoginRequest request);
}
