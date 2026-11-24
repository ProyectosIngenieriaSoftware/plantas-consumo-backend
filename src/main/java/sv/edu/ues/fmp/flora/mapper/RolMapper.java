package sv.edu.ues.fmp.flora.mapper;

import org.springframework.stereotype.Component;

import sv.edu.ues.fmp.flora.dto.request.RolRequest;
import sv.edu.ues.fmp.flora.dto.response.RolResponse;
import sv.edu.ues.fmp.flora.entity.Rol;

@Component
public class RolMapper {

    /**
     * No setea {@code esSistema} a proposito: un rol creado por la API nace
     * siempre en {@code false}, por el {@code @Builder.Default} de la entidad y
     * por el DEFAULT de la columna. Marcar un rol como de sistema es una
     * migracion, no una llamada HTTP.
     */
    public Rol toEntity(RolRequest request) {
        return Rol.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .build();
    }

    /**
     * Tampoco toca {@code esSistema} ni {@code activo}: la marca no se cambia
     * por ninguna ruta y el estado tiene sus propias vias (el DELETE y el
     * PATCH /activar). Que el nombre si se escriba aqui es lo que obligo a
     * SEG-H09 R7b: en un rol de sistema el servicio rechaza el cambio de
     * nombre antes de llegar a este metodo.
     */
    public void updateEntity(Rol entity, RolRequest request) {
        entity.setNombre(request.getNombre());
        entity.setDescripcion(request.getDescripcion());
    }

    public RolResponse toResponse(Rol entity) {
        return RolResponse.builder()
                .id(entity.getIdRol())
                .nombre(entity.getNombre())
                .descripcion(entity.getDescripcion())
                .activo(entity.getActivo())
                // Solo lectura: el frontend la necesita para bloquear la fila
                // protegida. Ver RolResponse.esSistema.
                .esSistema(entity.getEsSistema())
                .build();
    }
}
