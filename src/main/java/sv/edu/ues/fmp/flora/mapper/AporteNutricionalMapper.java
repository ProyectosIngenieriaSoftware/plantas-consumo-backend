package sv.edu.ues.fmp.flora.mapper;

import java.math.RoundingMode;
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
                .cantidad(request.getCantidad() != null ? request.getCantidad().setScale(4, RoundingMode.HALF_UP) : null)
                .unidadMedida(request.getUnidadMedida() != null && !request.getUnidadMedida().trim().isEmpty() ? request.getUnidadMedida().trim() : null)
                .porcionReferencia(request.getPorcionReferencia().trim())
                .observacion(request.getObservacion() != null && !request.getObservacion().trim().isEmpty() ? request.getObservacion().trim() : null)
                // Un aporte nuevo nace activo, y el cuerpo no puede decir otra
                // cosa: AporteNutricionalRequest no tiene campo activo. Se fija
                // aqui a mano en lugar de confiar en el @Builder.Default de la
                // entidad, por lo que se encontro en beneficio --un
                // @Builder.Default no sobrevive a la deserializacion de
                // Jackson-- y para que el valor no dependa de por que camino se
                // construyo el objeto.
                .activo(true)
                .build();
    }

    public void updateEntity(AporteNutricional entity, AporteNutricionalRequest request, EspecieParteComestible especieParte, Nutriente nutriente, Fuente fuente) {
        entity.setEspecieParteComestible(especieParte);
        entity.setNutriente(nutriente);
        entity.setFuente(fuente);
        entity.setCantidad(request.getCantidad() != null ? request.getCantidad().setScale(4, RoundingMode.HALF_UP) : null);
        entity.setUnidadMedida(request.getUnidadMedida() != null && !request.getUnidadMedida().trim().isEmpty() ? request.getUnidadMedida().trim() : null);
        entity.setPorcionReferencia(request.getPorcionReferencia().trim());
        entity.setObservacion(request.getObservacion() != null && !request.getObservacion().trim().isEmpty() ? request.getObservacion().trim() : null);
        // activo no se toca a proposito: el PUT edita la medicion, no el
        // estado. Quien da de baja es DELETE /{id} y quien reactiva es
        // PATCH /{id}/activar. Si el update lo pusiera en true, editar un
        // aporte retirado lo devolveria al catalogo sin que nadie lo pidiera.
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
                .activo(entity.getActivo())
                .build();
    }
}