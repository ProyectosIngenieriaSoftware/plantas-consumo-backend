package sv.edu.ues.fmp.flora.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.dto.request.AporteNutricionalRequest;
import sv.edu.ues.fmp.flora.dto.response.AporteNutricionalResponse;
import sv.edu.ues.fmp.flora.entity.AporteNutricional;
import sv.edu.ues.fmp.flora.entity.Fuente;
import sv.edu.ues.fmp.flora.entity.Nutriente;
import sv.edu.ues.fmp.flora.exception.RecursoDuplicadoException;
import sv.edu.ues.fmp.flora.exception.RecursoNoEncontradoException;
import sv.edu.ues.fmp.flora.mapper.AporteNutricionalMapper;
import sv.edu.ues.fmp.flora.repository.AporteNutricionalRepository;
import sv.edu.ues.fmp.flora.repository.FuenteRepository;
import sv.edu.ues.fmp.flora.repository.NutrienteRepository;
import sv.edu.ues.fmp.flora.service.AporteNutricionalService;

@Service
@RequiredArgsConstructor
public class AporteNutricionalServiceImpl implements AporteNutricionalService {

    private final AporteNutricionalRepository aporteRepository;
    private final NutrienteRepository nutrienteRepository;
    private final FuenteRepository fuenteRepository;
    private final AporteNutricionalMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<AporteNutricionalResponse> listarPorEspecieParte(Long idEspecieParte) {
        List<AporteNutricionalResponse> respuestas = new ArrayList<>();
        for (AporteNutricional entidad : aporteRepository.findByIdEspecieParteOrderByIdAporteAsc(idEspecieParte)) {
            respuestas.add(mapper.toResponse(entidad));
        }
        return respuestas;
    }

    @Override
    @Transactional(readOnly = true)
    public AporteNutricionalResponse obtenerPorId(Long id) {
        return mapper.toResponse(buscarOFallar(id));
    }

    @Override
    @Transactional
    public AporteNutricionalResponse crear(AporteNutricionalRequest request) {
        if (aporteRepository.existsByIdEspecieParteAndNutrienteIdNutriente(request.getIdEspecieParte(), request.getIdNutriente())) {
            throw new RecursoDuplicadoException("Esta parte de la planta ya tiene registrado el nutriente proporcionado.");
        }

        Nutriente nutriente = obtenerNutrienteOFallar(request.getIdNutriente());
        Fuente fuente = obtenerFuenteOFallar(request.getIdFuente());

        AporteNutricional nuevo = mapper.toEntity(request, nutriente, fuente);
        AporteNutricional guardado = aporteRepository.save(nuevo);
        return mapper.toResponse(guardado);
    }

    @Override
    @Transactional
    public AporteNutricionalResponse actualizar(Long id, AporteNutricionalRequest request) {
        AporteNutricional entidad = buscarOFallar(id);

        Optional<AporteNutricional> duplicado = aporteRepository
                .findByIdEspecieParteAndNutrienteIdNutriente(request.getIdEspecieParte(), request.getIdNutriente());

        if (duplicado.isPresent() && !duplicado.get().getIdAporte().equals(id)) {
            throw new RecursoDuplicadoException("Esta parte de la planta ya tiene registrado el nutriente proporcionado en otro registro.");
        }

        Nutriente nutriente = obtenerNutrienteOFallar(request.getIdNutriente());
        Fuente fuente = obtenerFuenteOFallar(request.getIdFuente());

        mapper.updateEntity(entidad, request, nutriente, fuente);
        return mapper.toResponse(entidad);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        AporteNutricional entidad = buscarOFallar(id);
        aporteRepository.delete(entidad);
    }

    private AporteNutricional buscarOFallar(Long id) {
        return aporteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un aporte nutricional con id " + id));
    }

    private Nutriente obtenerNutrienteOFallar(Long idNutriente) {
        return nutrienteRepository.findById(idNutriente)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el nutriente con id " + idNutriente));
    }

    private Fuente obtenerFuenteOFallar(Long idFuente) {
        return fuenteRepository.findById(idFuente)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la fuente con id " + idFuente));
    }
}