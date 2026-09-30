package sv.edu.ues.fmp.flora.service.impl;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.dto.request.AporteNutricionalRequest;
import sv.edu.ues.fmp.flora.dto.response.AporteNutricionalResponse;
import sv.edu.ues.fmp.flora.entity.AporteNutricional;
import sv.edu.ues.fmp.flora.entity.EspecieParteComestible;
import sv.edu.ues.fmp.flora.entity.Fuente;
import sv.edu.ues.fmp.flora.entity.Nutriente;
import sv.edu.ues.fmp.flora.exception.EstadoInvalidoException;
import sv.edu.ues.fmp.flora.exception.RecursoDuplicadoException;
import sv.edu.ues.fmp.flora.exception.RecursoNoEncontradoException;
import sv.edu.ues.fmp.flora.mapper.AporteNutricionalMapper;
import sv.edu.ues.fmp.flora.repository.AporteNutricionalRepository;
import sv.edu.ues.fmp.flora.repository.EspecieParteComestibleRepository;
import sv.edu.ues.fmp.flora.repository.FuenteRepository;
import sv.edu.ues.fmp.flora.repository.NutrienteRepository;
import sv.edu.ues.fmp.flora.service.AporteNutricionalService;

@Service
@RequiredArgsConstructor
public class AporteNutricionalServiceImpl implements AporteNutricionalService {

    private final AporteNutricionalRepository aporteRepository;
    private final EspecieParteComestibleRepository especieParteRepository;
    private final NutrienteRepository nutrienteRepository;
    private final FuenteRepository fuenteRepository;
    private final AporteNutricionalMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<AporteNutricionalResponse> listarPorEspecieParte(Long idEspecieParte) {
        return aporteRepository.findByEspecieParteComestibleIdEspecieParteOrderByIdAporteAsc(idEspecieParte)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AporteNutricionalResponse> listarPorNutriente(Long idNutriente) {
        return aporteRepository.findByNutrienteIdNutrienteOrderByIdAporteAsc(idNutriente)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AporteNutricionalResponse obtenerPorId(Long id) {
        return mapper.toResponse(buscarOFallar(id));
    }

    @Override
    @Transactional
    public AporteNutricionalResponse crear(AporteNutricionalRequest request) {
        if (aporteRepository.existsByEspecieParteComestibleIdEspecieParteAndNutrienteIdNutriente(
                request.getIdEspecieParte(), request.getIdNutriente())) {
            throw new RecursoDuplicadoException("Esta parte de la planta ya tiene registrado el nutriente proporcionado.");
        }

        EspecieParteComestible especieParte = obtenerEspecieParteOFallar(request.getIdEspecieParte());
        Nutriente nutriente = obtenerNutrienteOFallar(request.getIdNutriente());
        Fuente fuente = obtenerFuenteOFallar(request.getIdFuente());

        AporteNutricional nuevo = mapper.toEntity(request, especieParte, nutriente, fuente);
        return mapper.toResponse(aporteRepository.save(nuevo));
    }

    @Override
    @Transactional
    public AporteNutricionalResponse actualizar(Long id, AporteNutricionalRequest request) {
        AporteNutricional entidad = buscarOFallar(id);

        Optional<AporteNutricional> duplicado = aporteRepository
                .findByEspecieParteComestibleIdEspecieParteAndNutrienteIdNutriente(request.getIdEspecieParte(), request.getIdNutriente());

        if (duplicado.isPresent() && !duplicado.get().getIdAporte().equals(id)) {
            throw new RecursoDuplicadoException("Esta parte de la planta ya tiene registrado el nutriente proporcionado en otro registro.");
        }

        EspecieParteComestible especieParte = obtenerEspecieParteOFallar(request.getIdEspecieParte());
        Nutriente nutriente = obtenerNutrienteOFallar(request.getIdNutriente());
        Fuente fuente = obtenerFuenteOFallar(request.getIdFuente());

        mapper.updateEntity(entidad, request, especieParte, nutriente, fuente);
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

    private EspecieParteComestible obtenerEspecieParteOFallar(Long id) {
        EspecieParteComestible ep = especieParteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la parte comestible con id " + id));
        if (!Boolean.TRUE.equals(ep.getActiva())) {
            throw new EstadoInvalidoException("La parte comestible está desactivada y no puede recibir aportes.");
        }
        return ep;
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