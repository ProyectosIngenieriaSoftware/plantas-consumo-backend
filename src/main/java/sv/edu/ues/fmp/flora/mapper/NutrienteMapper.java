package sv.edu.ues.fmp.flora.mapper;

import org.springframework.stereotype.Component;
import sv.edu.ues.fmp.flora.dto.request.NutrienteRequest;
import sv.edu.ues.fmp.flora.dto.response.NutrienteResponse;
import sv.edu.ues.fmp.flora.entity.Nutriente;

@Component
public class NutrienteMapper {

    // Convierte DTOs a entidades
    public Nutriente toEntity(NutrienteRequest request) {
        return Nutriente.builder()
                .nombre(request.getNombre())
                .categoria(request.getCategoria())
                .descripcion(request.getDescripcion())
                .build();
    }

    // Actualiza la entidad existente
    public void updateEntity(Nutriente entity, NutrienteRequest request) {
        entity.setNombre(request.getNombre());
        entity.setCategoria(request.getCategoria());
        entity.setDescripcion(request.getDescripcion());
    }

    // Entidad a DTO de respuesta
    public NutrienteResponse toResponse(Nutriente entity) {
        return NutrienteResponse.builder()
                .idNutriente(entity.getIdNutriente())
                .nombre(entity.getNombre())
                .categoria(entity.getCategoria())
                .descripcion(entity.getDescripcion())
                .activo(entity.getActivo())
                .build();
    }
}
