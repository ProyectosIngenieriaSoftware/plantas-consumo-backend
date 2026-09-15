package sv.edu.ues.fmp.flora.mapper;

import org.springframework.stereotype.Component;
import sv.edu.ues.fmp.flora.dto.request.MunicipioRequest;
import sv.edu.ues.fmp.flora.dto.request.MunicipioResponse;
import sv.edu.ues.fmp.flora.entity.Departamento;
import sv.edu.ues.fmp.flora.entity.Municipio;

@Component
public class MunicipioMapper {

    /** Recibe el Departamento YA validado por el servicio. */
    public Municipio toEntity(MunicipioRequest request, Departamento departamento) {
        return Municipio.builder()
                .departamento(departamento)
                .nombre(request.getNombre())
                .build();
    }

    public void updateEntity(Municipio entity, MunicipioRequest request, Departamento departamento) {
        entity.setDepartamento(departamento);
        entity.setNombre(request.getNombre());
    }

    public MunicipioResponse toResponse(Municipio entity) {
        return MunicipioResponse.builder()
                .idMunicipio(entity.getIdMunicipio())
                .nombre(entity.getNombre())
                .idDepartamento(entity.getDepartamento().getIdDepartamento())
                .nombreDepartamento(entity.getDepartamento().getNombre())
                .activo(entity.getActivo())
                .build();
    }
}