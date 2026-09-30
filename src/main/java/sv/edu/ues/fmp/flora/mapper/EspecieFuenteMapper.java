package sv.edu.ues.fmp.flora.mapper;

import org.springframework.stereotype.Component;

import sv.edu.ues.fmp.flora.dto.request.EspecieFuenteRequest;
import sv.edu.ues.fmp.flora.dto.response.EspecieFuenteResponse;
import sv.edu.ues.fmp.flora.entity.Especie;
import sv.edu.ues.fmp.flora.entity.EspecieFuente;
import sv.edu.ues.fmp.flora.entity.EspecieFuenteId;
import sv.edu.ues.fmp.flora.entity.Fuente;

@Component
public class EspecieFuenteMapper {

    public EspecieFuente toEntity(EspecieFuenteRequest request, Especie especie, Fuente fuente) {
        return EspecieFuente.builder()
                .id(new EspecieFuenteId(request.getIdEspecie(), request.getIdFuente()))
                .especie(especie)
                .fuente(fuente)
                .observacion(request.getObservacion())
                .build();
    }

    public void updateEntity(EspecieFuente entity, EspecieFuenteRequest request) {
        entity.setObservacion(request.getObservacion());
    }

    public EspecieFuenteResponse toResponse(EspecieFuente entity) {
        return EspecieFuenteResponse.builder()
                .idEspecie(entity.getId().getIdEspecie())
                .idFuente(entity.getId().getIdFuente())
                .tituloFuente(entity.getFuente().getTitulo())
                .tipoFuente(entity.getFuente().getTipoFuente())
                .observacion(entity.getObservacion())
                .build();
    }
}