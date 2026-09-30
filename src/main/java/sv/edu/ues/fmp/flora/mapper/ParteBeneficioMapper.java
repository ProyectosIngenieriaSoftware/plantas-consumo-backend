package sv.edu.ues.fmp.flora.mapper;

import org.springframework.stereotype.Component;

import sv.edu.ues.fmp.flora.dto.request.ParteBeneficioRequest;
import sv.edu.ues.fmp.flora.dto.request.ParteBeneficioUpdateRequest;
import sv.edu.ues.fmp.flora.dto.response.ParteBeneficioResponse;
import sv.edu.ues.fmp.flora.entity.Beneficio;
import sv.edu.ues.fmp.flora.entity.EspecieParteComestible;
import sv.edu.ues.fmp.flora.entity.Fuente;
import sv.edu.ues.fmp.flora.entity.ParteBeneficio;
import sv.edu.ues.fmp.flora.entity.enums.ParteBeneficioId;

@Component
public class ParteBeneficioMapper {

    public ParteBeneficio toEntity(
            ParteBeneficioRequest request,
            EspecieParteComestible parteComestible,
            Beneficio beneficio,
            Fuente fuente
    ) {
        return ParteBeneficio.builder()
                .id(new ParteBeneficioId(
                        request.getIdEspecieParte(),
                        request.getIdBeneficio()
                ))
                .especieParteComestible(parteComestible)
                .beneficio(beneficio)
                .fuente(fuente)
                .observacion(normalizar(request.getObservacion()))
                .activa(true)
                .build();
    }

    public ParteBeneficioResponse toResponse(ParteBeneficio entidad) {
        return ParteBeneficioResponse.builder()
                .idEspecieParte(entidad.getId().getIdEspecieParte())
                .idBeneficio(entidad.getId().getIdBeneficio())
                .idFuente(
                        entidad.getFuente() == null
                                ? null
                                : entidad.getFuente().getIdFuente()
                )
                .observacion(entidad.getObservacion())
                .activa(entidad.getActiva())
                .build();
    }

    public void updateEntity(
            ParteBeneficio entidad,
            ParteBeneficioUpdateRequest request,
            Fuente fuente
    ) {
        entidad.setFuente(fuente);
        entidad.setObservacion(normalizar(request.getObservacion()));

        if (request.getActiva() != null) {
            entidad.setActiva(request.getActiva());
        }
    }

    private String normalizar(String texto) {
        return texto == null ? null : texto.trim();
    }
}