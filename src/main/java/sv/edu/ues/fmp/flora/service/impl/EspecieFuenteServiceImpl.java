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
    public EspecieFuenteResponse registrar(EspecieFuenteRequest request) {
        EspecieFuenteId id = new EspecieFuenteId(request.getIdEspecie(), request.getIdFuente());

        if (especieFuenteRepository.existsById(id)) {
            throw new RecursoDuplicadoException("La fuente ya se encuentra vinculada a esta especie");
        }

        Especie especie = especieRepository.findById(request.getIdEspecie())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una especie con id " + request.getIdEspecie()));

        Fuente fuente = fuenteRepository.findById(request.getIdFuente())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una fuente con id " + request.getIdFuente()));

        normalizar(request);
        EspecieFuente entidad = mapper.toEntity(request, especie, fuente);

        return mapper.toResponse(especieFuenteRepository.save(entidad));
    }

    @Override
    @Transactional
    public EspecieFuenteResponse actualizar(Long idEspecie, Long idFuente, EspecieFuenteRequest request) {
        // Validar que no intenten inyectar IDs cruzados desde la URL y el JSON
        if (!idEspecie.equals(request.getIdEspecie()) || !idFuente.equals(request.getIdFuente())) {
            throw new IllegalArgumentException("Los identificadores de la URL no coinciden con los del cuerpo de la petición");
        }

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
            String limpio = request.getObservacion().trim().replaceAll("\\s+", " ");
            request.setObservacion(limpio.isEmpty() ? null : limpio);
        }
    }
}