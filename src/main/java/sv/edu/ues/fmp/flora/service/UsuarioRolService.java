package sv.edu.ues.fmp.flora.service;

import java.util.List;

import sv.edu.ues.fmp.flora.dto.request.UsuarioRolRequest;
import sv.edu.ues.fmp.flora.dto.response.PermisoResponse;
import sv.edu.ues.fmp.flora.dto.response.RolResponse;
import sv.edu.ues.fmp.flora.dto.response.UsuarioRolResponse;

public interface UsuarioRolService {

    /**
     * Roles asignados al usuario.
     *
     * @param soloActivos true devuelve solo los roles vigentes; false (el
     *                    valor por omision) devuelve el historico completo,
     *                    incluidos los roles desactivados, que es util para
     *                    auditar que se le asigno al usuario alguna vez
     */
    List<RolResponse> obtenerRolesDeUsuario(Long idUsuario, boolean soloActivos);

    /**
     * Permisos que el usuario ejerce de verdad: solo los que le otorgan roles
     * activos, y solo los permisos que a su vez estan activos.
     * <p>
     * Un rol desactivado no otorga nada, que es justo la razon por la que se
     * desactivo; lo mismo vale para un permiso desactivado. Por eso esta lista
     * no coincide con recorrer {@link #obtenerRolesDeUsuario} y juntar sus
     * permisos: ahi aparece el historico, aqui lo que esta vigente. Es el dato
     * con el que se construyen las autoridades de un usuario autenticado, asi
     * que incluir un rol dado de baja seria conceder permisos revocados.
     */
    List<PermisoResponse> obtenerPermisosEfectivosDeUsuario(Long idUsuario);

    UsuarioRolResponse asignar(UsuarioRolRequest request);

    void quitar(Long idUsuario, Long idRol);
}
