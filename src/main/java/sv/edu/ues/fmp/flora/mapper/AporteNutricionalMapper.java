package sv.edu.ues.fmp.flora.mapper;

import org.springframework.stereotype.Component;
import sv.edu.ues.fmp.flora.dto.request.AporteNutricionalRequest;
import sv.edu.ues.fmp.flora.dto.response.AporteNutricionalResponse;
import sv.edu.ues.fmp.flora.entity.AporteNutricional;
import sv.edu.ues.fmp.flora.entity.Fuente;
import sv.edu.ues.fmp.flora.entity.Nutriente;

@Component
public class AporteNutricionalMapper {

    public AporteNutricional toEntity(AporteNutricionalRequest request, Nutriente nutriente, Fuente fuente) {
        return AporteNutricional.builder()
                .idEspecieParte(request.getIdEspecieParte())
                .nutriente(nutriente)
                .fuente(fuente)
                .cantidad(request.getCantidad())
                .unidadMedida(request.getUnidadMedida().trim())
                .porcionReferencia(request.getPorcionReferencia().trim())
                .observacion(request.getObservacion() != null && !request.getObservacion().trim().isEmpty()
                        ? request.getObservacion().trim() : null)
                .build();
    }

    public void updateEntity(AporteNutricional entity, AporteNutricionalRequest request, Nutriente nutriente, Fuente fuente) {
        entity.setIdEspecieParte(request.getIdEspecieParte());
        entity.setNutriente(nutriente);
        entity.setFuente(fuente);
        entity.setCantidad(request.getCantidad());
        entity.setUnidadMedida(request.getUnidadMedida().trim());
        entity.setPorcionReferencia(request.getPorcionReferencia().trim());
        entity.setObservacion(request.getObservacion() != null && !request.getObservacion().trim().isEmpty()
                ? request.getObservacion().trim() : null);
    }

    public AporteNutricionalResponse toResponse(AporteNutricional entity) {
        return AporteNutricionalResponse.builder()
                .idAporte(entity.getIdAporte())
                .idEspecieParte(entity.getIdEspecieParte())
                .idNutriente(entity.getNutriente().getIdNutriente())
                .nutrienteNombre(entity.getNutriente().getNombre())
                .idFuente(entity.getFuente().getIdFuente())
                .fuenteTitulo(entity.getFuente().getTitulo())
                .cantidad(entity.getCantidad())
                .unidadMedida(entity.getUnidadMedida())
                .porcionReferencia(entity.getPorcionReferencia())
                .observacion(entity.getObservacion())
                .build();
    }
}