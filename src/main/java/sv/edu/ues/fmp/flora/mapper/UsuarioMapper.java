package sv.edu.ues.fmp.flora.mapper;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.dto.request.UsuarioCambioCredencialesRequest;
import sv.edu.ues.fmp.flora.dto.request.UsuarioCreationRequest;
import sv.edu.ues.fmp.flora.dto.request.UsuarioUpdateRequest;
import sv.edu.ues.fmp.flora.dto.response.UsuarioResponse;
import sv.edu.ues.fmp.flora.entity.Usuario;

@Component
@RequiredArgsConstructor
public class UsuarioMapper {

    private final PasswordEncoder passwordEncoder;

    public Usuario toEntity(UsuarioCreationRequest request) {
        return Usuario.builder()
                .nombres(request.nombres())
                .apellidos(request.apellidos())
                .correo(request.correo())
                .nombreUsuario(request.nombreUsuario())
                .claveHash(passwordEncoder.encode(request.clave()))
                .build();
    }

    /**
     * La edicion por administrador: nombres y apellidos, nada mas.
     *
     * <h2>Por que ya no escribe correo ni nombreUsuario</h2>
     * Eran la raiz de una suplantacion. {@code POST
     * /api/usuarios/recuperar-clave} es publico y confia en el correo de la
     * cuenta, asi que quien pudiera reescribir el correo de otro usuario podia
     * apuntarlo a su propio buzon, pedir la recuperacion y quedarse con esa
     * cuenta --la del superadministrador incluida--. Cambiar las credenciales
     * de identidad paso a ser autoservicio del dueño y con su clave:
     * {@code UsuarioService.cambiarCredenciales}. Aqui se quedan los dos campos
     * que un administrador si corrige de otro: el nombre y el apellido.
     *
     * <h2>Lo que nunca escribio y sigue sin escribir</h2>
     * {@code activo}, {@code es_superadmin}, {@code clave_hash} y los roles.
     * Cada uno tiene su propia via con sus propias reglas, o ninguna via.
     */
    public void updateEntity(Usuario entity, UsuarioUpdateRequest request) {
        entity.setNombres(request.nombres());
        entity.setApellidos(request.apellidos());
    }

    /**
     * El autoservicio de credenciales. Es el unico sitio donde {@code correo} y
     * {@code nombreUsuario} se escriben despues de la creacion, y el servicio
     * solo llega aqui tras comprobar la clave actual del dueño.
     */
    public void updateCredenciales(Usuario entity, UsuarioCambioCredencialesRequest request) {
        entity.setCorreo(request.correo());
        entity.setNombreUsuario(request.nombreUsuario());
    }

    public UsuarioResponse toResponse(Usuario entity) {
        return UsuarioResponse.builder()
                .id(entity.getIdUsuario())
                .nombres(entity.getNombres())
                .apellidos(entity.getApellidos())
                .correo(entity.getCorreo())
                .nombreUsuario(entity.getNombreUsuario())
                .activo(entity.getActivo())
                // Solo lectura: el frontend la necesita para bloquear la fila
                // protegida. Ver UsuarioResponse.esSuperadmin.
                .esSuperadmin(entity.getEsSuperadmin())
                .fechaRegistro(entity.getFechaRegistro())
                .fechaActualizacion(entity.getFechaActualizacion())
                .ultimoAcceso(entity.getUltimoAcceso())
                .build();
    }
}
