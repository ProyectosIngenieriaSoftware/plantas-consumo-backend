package sv.edu.ues.fmp.flora.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.dto.request.RolPermisoRequest;
import sv.edu.ues.fmp.flora.dto.response.PermisoResponse;
import sv.edu.ues.fmp.flora.dto.response.RolPermisoResponse;
import sv.edu.ues.fmp.flora.entity.Permiso;
import sv.edu.ues.fmp.flora.entity.Rol;
import sv.edu.ues.fmp.flora.entity.RolPermiso;
import sv.edu.ues.fmp.flora.entity.enums.RolPermisoId;
import sv.edu.ues.fmp.flora.exception.EstadoInvalidoException;
import sv.edu.ues.fmp.flora.exception.IdInvalidoException;
import sv.edu.ues.fmp.flora.exception.RecursoDuplicadoException;
import sv.edu.ues.fmp.flora.exception.RecursoNoEncontradoException;
import sv.edu.ues.fmp.flora.mapper.PermisoMapper;
import sv.edu.ues.fmp.flora.mapper.RolPermisoMapper;
import sv.edu.ues.fmp.flora.repository.PermisoRepository;
import sv.edu.ues.fmp.flora.repository.RolPermisoRepository;
import sv.edu.ues.fmp.flora.repository.RolRepository;
import sv.edu.ues.fmp.flora.service.ProteccionDeAdministracion;
import sv.edu.ues.fmp.flora.service.RolPermisoService;

@Service
@RequiredArgsConstructor
public class RolPermisoServiceImpl implements RolPermisoService {

    private final RolPermisoRepository rolPermisoRepository;
    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final PermisoMapper permisoMapper;
    private final RolPermisoMapper rolPermisoMapper;

    /**
     * SEG-H09 R7a: solo el superadministrador cambia los permisos de un rol de
     * sistema. Aqui no se usa R7b --el nombre y la fila del rol no se tocan
     * desde este servicio-- ni las reglas de usuario: cambiar los permisos de
     * un rol es exactamente lo que el superadministrador tiene que poder
     * hacer, y lo que nadie mas debe.
     */
    private final ProteccionDeAdministracion proteccionDeAdministracion;

    @Override
    @Transactional(readOnly = true)
    public List<PermisoResponse> obtenerPermisosDeRol(Long idRol) {
        validarId(idRol, "rol");
        if (!rolRepository.existsById(idRol)) {
            throw new RecursoNoEncontradoException("No existe un rol con id " + idRol);
        }

        return rolPermisoRepository.findPermisosByRolId(idRol).stream()
                .map(permisoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RolPermisoResponse asignar(RolPermisoRequest request) {
        Rol rol = rolRepository.findById(request.getIdRol())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un rol con id " + request.getIdRol()));

        // SEG-H09 R7a: agregarle un permiso a un rol de sistema solo lo puede
        // el superadministrador. Era la segunda via del agujero, la que no
        // necesitaba renombrar nada: con USUARIO_GESTIONAR se podian armar los
        // permisos del rol ADMINISTRADOR a voluntad, o --por .quitar-- vaciarlo
        // y degradar a todos los administradores, superadmin incluido, de una
        // sola peticion.
        //
        // Va justo despues de resolver el rol y ANTES de buscar el permiso: la
        // regla habla del rol, que es el recurso que se muta, y a quien no
        // puede tocarlo no se le confirma de paso si tal id de permiso existe.
        // Sin R7b: cambiar los permisos de un rol de sistema es exactamente lo
        // que el superadministrador SI tiene que poder hacer.
        proteccionDeAdministracion.verificarMutacionDeRol(rol);

        Permiso permiso = permisoRepository.findById(request.getIdPermiso())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un permiso con id " + request.getIdPermiso()));

        if (!Boolean.TRUE.equals(rol.getActivo())) {
            throw new EstadoInvalidoException(
                    "No se puede asignar un permiso al rol " + rol.getNombre() + " porque está desactivado");
        }
        if (!Boolean.TRUE.equals(permiso.getActivo())) {
            throw new EstadoInvalidoException(
                    "No se puede asignar el permiso " + permiso.getCodigo() + " porque está desactivado");
        }

        RolPermisoId id = new RolPermisoId(rol.getIdRol(), permiso.getIdPermiso());
        if (rolPermisoRepository.existsById(id)) {
            throw new RecursoDuplicadoException(
                    "El rol " + rol.getNombre() + " ya tiene asignado el permiso " + permiso.getCodigo());
        }

        RolPermiso nuevo = RolPermiso.builder()
                .id(id)
                .rol(rol)
                .permiso(permiso)
                .build();

        RolPermiso guardado = rolPermisoRepository.save(nuevo);
        return rolPermisoMapper.toResponse(guardado);
    }

    @Override
    @Transactional
    public void quitar(Long idRol, Long idPermiso) {
        validarId(idRol, "rol");
        validarId(idPermiso, "permiso");

        // El rol se resuelve ahora, cosa que este metodo no hacia: trabajaba
        // solo con los dos ids. Hace falta para SEG-H09 R7a --la regla
        // pregunta por el flag de la entidad-- y de paso alinea el 404 con el
        // de .asignar y .obtenerPermisosDeRol: un id de rol que no existe
        // responde "No existe un rol con id X" y no "el rol X no tiene
        // asignado el permiso Y", que decia lo mismo de un rol inexistente y
        // de uno real sin ese permiso.
        Rol rol = rolRepository.findById(idRol)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un rol con id " + idRol));

        // R7a: quitarle un permiso a un rol de sistema solo lo puede el
        // superadministrador. Es la mitad peligrosa de la pareja con .asignar:
        // vaciar de permisos el rol ADMINISTRADOR degrada a todos los
        // administradores a la vez sin tocar ninguna fila de usuario_rol, que
        // es la razon de que R7 se ancle en el rol y no en el usuario.
        proteccionDeAdministracion.verificarMutacionDeRol(rol);

        RolPermisoId id = new RolPermisoId(idRol, idPermiso);
        if (!rolPermisoRepository.existsById(id)) {
            throw new RecursoNoEncontradoException(
                    "El rol " + idRol + " no tiene asignado el permiso " + idPermiso);
        }

        rolPermisoRepository.deleteById(id);
    }

    private void validarId(Long id, String etiqueta) {
        if (id == null || id <= 0) {
            throw new IdInvalidoException("El id del " + etiqueta + " debe ser un valor positivo");
        }
    }
}
