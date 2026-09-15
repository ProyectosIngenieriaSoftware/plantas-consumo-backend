package sv.edu.ues.fmp.flora.mapper;

import org.springframework.stereotype.Component;

import sv.edu.ues.fmp.flora.dto.request.FuenteRequest;
import sv.edu.ues.fmp.flora.dto.response.FuenteResponse;
import sv.edu.ues.fmp.flora.entity.Fuente;

@Component
public class FuenteMapper {
    // convierte DTOs a entidades y viceversa

    public Fuente toEntity(FuenteRequest request) {
        return Fuente.builder()
                .titulo(request.getTitulo())
                .autor(request.getAutor())
                .anio(request.getAnio())
                .tipoFuente(request.getTipoFuente())
                .url(request.getUrl())
                .referenciaBibliografica(request.getReferenciaBibliografica())
                .build();
    }

    public void updateEntity(Fuente entity, FuenteRequest request) {
        entity.setTitulo(request.getTitulo());
        entity.setAutor(request.getAutor());
        entity.setAnio(request.getAnio());
        entity.setTipoFuente(request.getTipoFuente());
        entity.setUrl(request.getUrl());
        entity.setReferenciaBibliografica(request.getReferenciaBibliografica());
    }

    // entidad a dto
    public FuenteResponse toResponse(Fuente entity) {
        return FuenteResponse.builder()
                .idFuente(entity.getIdFuente())
                .titulo(entity.getTitulo())
                .autor(entity.getAutor())
                .anio(entity.getAnio())
                .tipoFuente(entity.getTipoFuente())
                .url(entity.getUrl())
                .referenciaBibliografica(entity.getReferenciaBibliografica())
                .activa(entity.getActiva())
                .build();
    }
}
