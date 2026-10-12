package sv.edu.ues.fmp.flora.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sv.edu.ues.fmp.flora.dto.request.UbicacionEspecieRequestDTO;
import sv.edu.ues.fmp.flora.dto.response.UbicacionEspecieResponseDTO;
import sv.edu.ues.fmp.flora.entity.Distrito;
import sv.edu.ues.fmp.flora.entity.Especie;
import sv.edu.ues.fmp.flora.entity.UbicacionEspecie;
import sv.edu.ues.fmp.flora.repository.DistritoRepository;
import sv.edu.ues.fmp.flora.repository.EspecieRepository;
import sv.edu.ues.fmp.flora.repository.UbicacionEspecieRepository;
import sv.edu.ues.fmp.flora.service.UbicacionEspecieService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UbicacionEspecieServiceImpl implements UbicacionEspecieService {

    private final UbicacionEspecieRepository ubicacionEspecieRepository;
    private final EspecieRepository especieRepository;
    private final DistritoRepository distritoRepository;

    @Override
    @Transactional
    public UbicacionEspecieResponseDTO registrarUbicacion(UbicacionEspecieRequestDTO dto) {
        Especie especie = especieRepository.findById(dto.getIdEspecie())
                .orElseThrow(() -> new EntityNotFoundException("Especie no encontrada"));

        Distrito distrito = distritoRepository.findById(dto.getIdDistrito())
                .orElseThrow(() -> new EntityNotFoundException("Distrito no encontrado"));

        UbicacionEspecie ubicacion = new UbicacionEspecie();
        ubicacion.setEspecie(especie);
        ubicacion.setDistrito(distrito);
        ubicacion.setNombreLugar(dto.getNombreLugar());
        ubicacion.setLatitud(dto.getLatitud());
        ubicacion.setLongitud(dto.getLongitud());
        ubicacion.setDescripcion(dto.getDescripcion());
        ubicacion.setFechaObservacion(dto.getFechaObservacion());

        UbicacionEspecie guardada = ubicacionEspecieRepository.save(ubicacion);

        return mapearAResponse(guardada);
    }

    @Override
    public List<UbicacionEspecieResponseDTO> obtenerUbicacionesPorEspecie(Long idEspecie) {
        return ubicacionEspecieRepository.findByEspecie_IdEspecieAndActivaTrue(idEspecie)
                .stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Override
    @Transactional
    public UbicacionEspecieResponseDTO actualizarUbicacion(Long idUbicacion, UbicacionEspecieRequestDTO dto) {
        UbicacionEspecie ubicacion = ubicacionEspecieRepository.findById(idUbicacion)
                .orElseThrow(() -> new EntityNotFoundException("Ubicación no encontrada con ID: " + idUbicacion));

        Especie especie = especieRepository.findById(dto.getIdEspecie())
                .orElseThrow(() -> new EntityNotFoundException("Especie no encontrada"));

        Distrito distrito = distritoRepository.findById(dto.getIdDistrito())
                .orElseThrow(() -> new EntityNotFoundException("Distrito no encontrado"));

        ubicacion.setEspecie(especie);
        ubicacion.setDistrito(distrito);
        ubicacion.setNombreLugar(dto.getNombreLugar());
        ubicacion.setLatitud(dto.getLatitud());
        ubicacion.setLongitud(dto.getLongitud());
        ubicacion.setDescripcion(dto.getDescripcion());
        ubicacion.setFechaObservacion(dto.getFechaObservacion());

        UbicacionEspecie actualizada = ubicacionEspecieRepository.save(ubicacion);
        return mapearAResponse(actualizada);
    }

    @Override
    @Transactional
    public void desactivarUbicacion(Long idUbicacion) {
        UbicacionEspecie ubicacion = ubicacionEspecieRepository.findById(idUbicacion)
                .orElseThrow(() -> new EntityNotFoundException("Ubicación no encontrada con ID: " + idUbicacion));

        ubicacion.setActiva(false);
        ubicacionEspecieRepository.save(ubicacion);
    }

    private UbicacionEspecieResponseDTO mapearAResponse(UbicacionEspecie entidad) {
        UbicacionEspecieResponseDTO response = new UbicacionEspecieResponseDTO();
        response.setIdUbicacion(entidad.getIdUbicacion());

        response.setIdEspecie(entidad.getEspecie().getIdEspecie());
        response.setIdDistrito(entidad.getDistrito().getIdDistrito());

        response.setNombreDistrito(entidad.getDistrito().getNombre());
        response.setNombreLugar(entidad.getNombreLugar());
        response.setLatitud(entidad.getLatitud());
        response.setLongitud(entidad.getLongitud());
        response.setDescripcion(entidad.getDescripcion());
        response.setFechaObservacion(entidad.getFechaObservacion());
        response.setActiva(entidad.getActiva());
        response.setFechaRegistro(entidad.getFechaRegistro());
        return response;
    }
}