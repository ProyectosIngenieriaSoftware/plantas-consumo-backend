package sv.edu.ues.fmp.flora.mapper;

import org.springframework.stereotype.Component;

import sv.edu.ues.fmp.flora.dto.request.HabitatRequest;
import sv.edu.ues.fmp.flora.dto.response.HabitatResponse;
import sv.edu.ues.fmp.flora.entity.Habitat;

@Component
public class HabitatMapper {

    public Habitat toEntity(HabitatRequest request) {
        return Habitat.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .build();
    }

    public void updateEntity(Habitat entity, HabitatRequest request) {
        entity.setNombre(request.getNombre());
        entity.setDescripcion(request.getDescripcion());
    }

    public HabitatResponse toResponse(Habitat entity) {
        return HabitatResponse.builder()
                .idHabitat(entity.getIdHabitat())
                .nombre(entity.getNombre())
                .descripcion(entity.getDescripcion())
                .activo(entity.getActivo())
                .build();
    }
}
