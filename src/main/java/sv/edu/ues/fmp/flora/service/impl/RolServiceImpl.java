package sv.edu.ues.fmp.flora.service.impl;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.dto.request.RolRequest;
import sv.edu.ues.fmp.flora.dto.response.PaginaResponse;
import sv.edu.ues.fmp.flora.dto.response.RolResponse;
import sv.edu.ues.fmp.flora.entity.Rol;
import sv.edu.ues.fmp.flora.exception.IdInvalidoException;
import sv.edu.ues.fmp.flora.exception.RecursoDuplicadoException;
import sv.edu.ues.fmp.flora.exception.RecursoNoEncontradoException;
import sv.edu.ues.fmp.flora.mapper.RolMapper;
import sv.edu.ues.fmp.flora.repository.RolRepository;
import sv.edu.ues.fmp.flora.service.ProteccionDeAdministracion;
import sv.edu.ues.fmp.flora.service.RolService;
import sv.edu.ues.fmp.flora.util.Paginacion;

@Service
@RequiredArgsConstructor
public class RolServiceImpl implements RolService {

    private final RolRepository rolRepository;
    private final RolMapper rolMapper;

    /**
     * Las reglas de SEG-H09 que hablan de roles.
     * <p>
     * Aqui no hay usuario objetivo, asi que las reglas de autoproteccion
     * (R1) y las de "quien le hace que a quien" (R2, R3, R4, R6) no entran.
     * Las que si: <strong>R7a</strong> --solo el superadministrador muta un
     * rol de sistema-- en las tres vias que lo mutan, <strong>R7b</strong>
     * --su nombre y su fila son intocables para todos-- en la edicion y en la
     * baja, y <strong>R5</strong> --nunca cero administradores activos-- en la
     * baja.
     */
    private final ProteccionDeAdministracion proteccionDeAdministracion;

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<RolResponse> listarTodos(int pagina, int tamanio) {
        Pageable pageable = Paginacion.armar(pagina, tamanio, "idRol");
        return Paginacion.aRespuesta(rolRepository.findAll(pageable), rolMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<RolResponse> listarActivos(int pagina, int tamanio) {
        Pageable pageable = Paginacion.armar(pagina, tamanio, "idRol");
        return Paginacion.aRespuesta(rolRepository.findByActivoTrue(pageable), rolMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RolResponse obtenerPorId(Long id) {
        return rolMapper.toResponse(buscarOFallar(id));
    }

    @Override
    @Transactional
    public RolResponse crear(RolRequest request) {
        if (rolRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new RecursoDuplicadoException(
                    "Ya existe un rol con el nombre " + request.getNombre());
        }

        Rol nuevo = rolMapper.toEntity(request);
        Rol guardado = rolRepository.save(nuevo);

        return rolMapper.toResponse(guardado);
    }

    @Override
    @Transactional
    public RolResponse actualizar(Long id, RolRequest request) {
        Rol entidad = buscarOFallar(id);

        // SEG-H09 R7, en sus dos caras y en este orden:
        //
        // R7a (403): si es un rol de sistema, solo lo toca el
        // superadministrador. Esta era LA via del agujero: PUT /api/roles/{id}
        // escribe rol.nombre, y renombrar ADMINISTRADOR apagaba R2, R3 y R5 de
        // una sola peticion, porque las tres preguntan por el nombre.
        //
        // R7b (409): el nombre de un rol de sistema no cambia ni para el
        // superadministrador. Va despues de R7a porque son preguntas
        // distintas: primero si quien llama puede tocar el rol, y solo
        // entonces si la operacion concreta existe.
        //
        // Las dos van despues de buscarOFallar, como R6 en el modulo de
        // usuario: un id inexistente sigue siendo 404 y no 403.
        proteccionDeAdministracion.verificarMutacionDeRol(entidad);
        proteccionDeAdministracion.verificarRenombradoDeRol(entidad, request.getNombre());

        Optional<Rol> conMismoNombre = rolRepository.findByNombreIgnoreCase(request.getNombre());
        if (conMismoNombre.isPresent() && !conMismoNombre.get().getIdRol().equals(id)) {
            throw new RecursoDuplicadoException(
                    "Ya existe otro rol con el nombre " + request.getNombre());
        }

        rolMapper.updateEntity(entidad, request);
        return rolMapper.toResponse(entidad);
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Rol entidad = buscarOFallar(id);

        // SEG-H09, las tres reglas que miran la baja de un rol, en este orden:
        //
        // R7a (403): si es un rol de sistema, solo el superadministrador llega
        // mas alla de aqui.
        //
        // R7b (409): y un rol de sistema no se da de baja NUNCA, el
        // superadministrador incluido.
        //
        // R5 (409): el invariante de "nunca cero administradores activos", que
        // esta tarea no toca. Va al final porque las prohibiciones absolutas
        // van primero --el mismo criterio con el que R1 y R4 van antes que
        // R5-- y porque no tiene sentido contar administradores en una
        // operacion que de todas formas no estaba permitida.
        //
        // El efecto del orden es que para el rol ADMINISTRADOR esta tercera
        // llamada ya no se alcanza: R7b corta antes. Se conserva porque sigue
        // siendo el invariante de cualquier rol que se llame ADMINISTRADOR sin
        // llevar el flag --una base donde V20 no marcara lo que debia-- y
        // porque es la mitad de R5 que no cambia.
        //
        // Las tres van despues de resolver la entidad: un id que no existe
        // sigue siendo 404. Para un rol personalizado ninguna hace nada.
        proteccionDeAdministracion.verificarMutacionDeRol(entidad);
        proteccionDeAdministracion.verificarBajaDeRol(entidad);
        proteccionDeAdministracion.verificarDesactivacionDeRol(entidad);

        entidad.setActivo(false);
    }

    @Override
    @Transactional
    public RolResponse reactivar(Long id) {
        Rol entidad = buscarOFallar(id);

        // SEG-H09 R7a. Reactivar tambien es mutar el rol, aunque sea en la
        // direccion segura: un rol de sistema que estuviera dado de baja --en
        // una base que llegara asi, porque desde R7b no se puede dar de baja--
        // solo lo vuelve a poner en pie el superadministrador. Sin R7b aqui:
        // reactivar no es renombrar ni eliminar, asi que el superadministrador
        // si puede.
        proteccionDeAdministracion.verificarMutacionDeRol(entidad);

        entidad.setActivo(true);
        return rolMapper.toResponse(entidad);
    }

    private Rol buscarOFallar(Long id) {
        validarId(id);
        return rolRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un rol con id " + id));
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) {
            throw new IdInvalidoException("El id del rol debe ser un valor positivo");
        }
    }
}
