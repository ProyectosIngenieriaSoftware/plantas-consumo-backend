package sv.edu.ues.fmp.flora.mapper;

import org.springframework.stereotype.Component;

import sv.edu.ues.fmp.flora.dto.request.PartePlantaRequest;
import sv.edu.ues.fmp.flora.dto.response.PartePlantaResponse;
import sv.edu.ues.fmp.flora.entity.PartePlanta;


@Component
public class PartePlantaMapper {


    public PartePlanta toEntity(PartePlantaRequest request) {
        return PartePlanta.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .build();
    }

    public void updateEntity(PartePlanta entity, PartePlantaRequest request) {
        entity.setNombre(request.getNombre());
        entity.setDescripcion(request.getDescripcion());
    }

    public PartePlantaResponse toResponse(PartePlanta entity) {
        return PartePlantaResponse.builder()
                .idPartePlanta(entity.getIdPartePlanta())
                .nombre(entity.getNombre())
                .descripcion(entity.getDescripcion())
                .activo(entity.getActivo())
                .build();
    }
}
