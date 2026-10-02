package sv.edu.ues.fmp.flora.mapper;

import org.springframework.stereotype.Component;
import sv.edu.ues.fmp.flora.dto.request.AporteNutricionalRequest;
import sv.edu.ues.fmp.flora.dto.response.AporteNutricionalResponse;
import sv.edu.ues.fmp.flora.entity.AporteNutricional;
import sv.edu.ues.fmp.flora.entity.EspecieParteComestible;
import sv.edu.ues.fmp.flora.entity.Fuente;
import sv.edu.ues.fmp.flora.entity.Nutriente;

@Component
public class AporteNutricionalMapper {

    public AporteNutricional toEntity(AporteNutricionalRequest request, EspecieParteComestible especieParte, Nutriente nutriente, Fuente fuente) {
        return AporteNutricional.builder()
                .especieParteComestible(especieParte)
                .nutriente(nutriente)
                .fuente(fuente)
                .cantidad(request.getCantidad())
                .unidadMedida(request.getUnidadMedida() != null && !request.getUnidadMedida().trim().isEmpty() ? request.getUnidadMedida().trim() : null)
                .porcionReferencia(request.getPorcionReferencia().trim())
                .observacion(request.getObservacion() != null && !request.getObservacion().trim().isEmpty() ? request.getObservacion().trim() : null)
                .build();
    }

    public void updateEntity(AporteNutricional entity, AporteNutricionalRequest request, EspecieParteComestible especieParte, Nutriente nutriente, Fuente fuente) {
        entity.setEspecieParteComestible(especieParte);
        entity.setNutriente(nutriente);
        entity.setFuente(fuente);
        entity.setCantidad(request.getCantidad());
        entity.setUnidadMedida(request.getUnidadMedida() != null && !request.getUnidadMedida().trim().isEmpty() ? request.getUnidadMedida().trim() : null);
        entity.setPorcionReferencia(request.getPorcionReferencia().trim());
        entity.setObservacion(request.getObservacion() != null && !request.getObservacion().trim().isEmpty() ? request.getObservacion().trim() : null);
    }

    public AporteNutricionalResponse toResponse(AporteNutricional entity) {
        return AporteNutricionalResponse.builder()
                .idAporte(entity.getIdAporte())
                .idEspecieParte(entity.getEspecieParteComestible().getIdEspecieParte())
                .especieNombreCientifico(entity.getEspecieParteComestible().getEspecie().getNombreCientifico())
                .parteNombre(entity.getEspecieParteComestible().getPartePlanta().getNombre())
                .idNutriente(entity.getNutriente().getIdNutriente())
                .nutrienteNombre(entity.getNutriente().getNombre())
                .idFuente(entity.getFuente() != null ? entity.getFuente().getIdFuente() : null)
                .fuenteTitulo(entity.getFuente() != null ? entity.getFuente().getTitulo() : null)
                .cantidad(entity.getCantidad())
                .unidadMedida(entity.getUnidadMedida())
                .porcionReferencia(entity.getPorcionReferencia())
                .observacion(entity.getObservacion())
                .build();
    }
}