package sv.edu.ues.fmp.flora.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.dto.request.EspecieFuenteRequest;
import sv.edu.ues.fmp.flora.dto.response.EspecieFuenteResponse;
import sv.edu.ues.fmp.flora.entity.Especie;
import sv.edu.ues.fmp.flora.entity.EspecieFuente;
import sv.edu.ues.fmp.flora.entity.EspecieFuenteId;
import sv.edu.ues.fmp.flora.entity.Fuente;
import sv.edu.ues.fmp.flora.exception.RecursoDuplicadoException;
import sv.edu.ues.fmp.flora.exception.RecursoNoEncontradoException;
import sv.edu.ues.fmp.flora.mapper.EspecieFuenteMapper;
import sv.edu.ues.fmp.flora.repository.EspecieFuenteRepository;
import sv.edu.ues.fmp.flora.repository.EspecieRepository;
import sv.edu.ues.fmp.flora.repository.FuenteRepository;
import sv.edu.ues.fmp.flora.service.EspecieFuenteService;

@Service
@RequiredArgsConstructor
public class EspecieFuenteServiceImpl implements EspecieFuenteService {

    private final EspecieFuenteRepository especieFuenteRepository;
    private final EspecieRepository especieRepository;
    private final FuenteRepository fuenteRepository;
    private final EspecieFuenteMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<EspecieFuenteResponse> listarPorEspecie(Long idEspecie) {
        if (!especieRepository.existsById(idEspecie)) {
            throw new RecursoNoEncontradoException("No existe una especie con id " + idEspecie);
        }

        return especieFuenteRepository.findByEspecie_IdEspecie(idEspecie).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EspecieFuenteResponse registrar(Long idEspecie, EspecieFuenteRequest request) {
        EspecieFuenteId id = new EspecieFuenteId(idEspecie, request.getIdFuente());

        if (especieFuenteRepository.existsById(id)) {
            throw new RecursoDuplicadoException("La fuente ya se encuentra vinculada a esta especie");
        }

        Especie especie = especieRepository.findById(idEspecie)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una especie con id " + idEspecie));

        if (Boolean.FALSE.equals(especie.getActiva())) {
            throw new RecursoNoEncontradoException("La especie se encuentra inactiva y no admite nuevas asociaciones");
        }

        Fuente fuente = fuenteRepository.findById(request.getIdFuente())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una fuente con id " + request.getIdFuente()));

        if (Boolean.FALSE.equals(fuente.getActiva())) {
            throw new RecursoNoEncontradoException("La fuente se encuentra inactiva y no puede ser vinculada");
        }

        normalizar(request);
        EspecieFuente entidad = mapper.toEntity(idEspecie, request, especie, fuente);

        return mapper.toResponse(especieFuenteRepository.save(entidad));
    }

    @Override
    @Transactional
    public EspecieFuenteResponse actualizar(Long idEspecie, Long idFuente, EspecieFuenteRequest request) {
        // Se quitó la validación cruzada y se confía en la ruta como fuente única
        EspecieFuenteId id = new EspecieFuenteId(idEspecie, idFuente);

        EspecieFuente entidad = especieFuenteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la relación entre la especie y fuente indicadas"));

        normalizar(request);
        mapper.updateEntity(entidad, request);

        return mapper.toResponse(especieFuenteRepository.save(entidad));
    }

    @Override
    @Transactional
    public void eliminar(Long idEspecie, Long idFuente) {
        EspecieFuenteId id = new EspecieFuenteId(idEspecie, idFuente);
        if (!especieFuenteRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("No existe la relación a eliminar");
        }
        especieFuenteRepository.deleteById(id);
    }

    private void normalizar(EspecieFuenteRequest request) {
        if (request.getObservacion() != null) {
            String limpio = request.getObservacion().strip();
            request.setObservacion(limpio.isEmpty() ? null : limpio);
        }
    }
}