package sv.edu.ues.fmp.flora.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.dto.request.PartePlantaRequest;
import sv.edu.ues.fmp.flora.dto.response.PartePlantaResponse;
import sv.edu.ues.fmp.flora.entity.PartePlanta;
import sv.edu.ues.fmp.flora.exception.RecursoDuplicadoException;
import sv.edu.ues.fmp.flora.exception.RecursoNoEncontradoException;
import sv.edu.ues.fmp.flora.mapper.PartePlantaMapper;
import sv.edu.ues.fmp.flora.repository.PartePlantaRepository;
import sv.edu.ues.fmp.flora.service.PartePlantaService;

/**
 * Implementacion de la logica de negocio de las partes de planta.
 * Aqui viven las reglas (unicidad del nombre, baja logica) y aqui termina el
 * recorrido de la entidad: hacia arriba solo salen DTOs.
 */
@Service
@RequiredArgsConstructor
public class PartePlantaServiceImpl implements PartePlantaService {

    // Inyeccion por constructor generada por @RequiredArgsConstructor sobre los
    // campos final: las dependencias quedan explicitas e inmutables.
    private final PartePlantaRepository partePlantaRepository;
    private final PartePlantaMapper partePlantaMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PartePlantaResponse> listarTodas() {
        List<PartePlantaResponse> respuestas = new ArrayList<>();
        for (PartePlanta entidad : partePlantaRepository.findAll()) {
            respuestas.add(partePlantaMapper.toResponse(entidad));
        }
        return respuestas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartePlantaResponse> listarActivas() {
        List<PartePlantaResponse> respuestas = new ArrayList<>();
        for (PartePlanta entidad : partePlantaRepository.findByActivoTrue()) {
            respuestas.add(partePlantaMapper.toResponse(entidad));
        }
        return respuestas;
    }

    @Override
    @Transactional(readOnly = true)
    public PartePlantaResponse obtenerPorId(Long id) {
        PartePlanta entidad = buscarOFallar(id);
        return partePlantaMapper.toResponse(entidad);
    }

    @Override
    @Transactional
    public PartePlantaResponse crear(PartePlantaRequest request) {
        // La base ya tiene UNIQUE sobre parte_planta.nombre, pero se valida aqui
        // igualmente: si dejaramos que reventara la base, el cliente recibiria un
        // 500 con una excepcion de driver. Validando antes devolvemos un 409
        // legible que explica exactamente que paso.
        if (partePlantaRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new RecursoDuplicadoException(
                    "Ya existe una parte de planta con el nombre " + request.getNombre());
        }

        PartePlanta nueva = partePlantaMapper.toEntity(request);

        // En crear si hace falta save(): la entidad es nueva y todavia no esta
        // gestionada por el contexto de persistencia.
        PartePlanta guardada = partePlantaRepository.save(nueva);

        return partePlantaMapper.toResponse(guardada);
    }

    @Override
    @Transactional
    public PartePlantaResponse actualizar(Long id, PartePlantaRequest request) {
        PartePlanta entidad = buscarOFallar(id);

        // El nombre puede repetirse consigo mismo (el usuario quiza solo cambio la
        // descripcion), pero no puede pisar el nombre de OTRO registro.
        Optional<PartePlanta> conMismoNombre =
                partePlantaRepository.findByNombreIgnoreCase(request.getNombre());
        if (conMismoNombre.isPresent()
                && !conMismoNombre.get().getIdPartePlanta().equals(id)) {
            throw new RecursoDuplicadoException(
                    "Ya existe otra parte de planta con el nombre " + request.getNombre());
        }

        partePlantaMapper.updateEntity(entidad, request);

        // No se llama a repository.save() a proposito. La entidad se obtuvo dentro
        // de esta transaccion, asi que esta gestionada (managed) por el contexto de
        // persistencia de Hibernate. Al hacer commit, Hibernate compara el estado
        // actual con el que leyo al cargarla (dirty checking) y emite el UPDATE por
        // su cuenta. Llamar a save() aqui seria redundante.
        return partePlantaMapper.toResponse(entidad);
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        PartePlanta entidad = buscarOFallar(id);

        // Borrado LOGICO, nunca delete() fisico: otras tablas referencian
        // parte_planta por clave foranea y un DELETE romperia esas referencias.
        // Ademas, en un sistema de conocimiento cientifico la informacion no se
        // destruye: se marca como no vigente y queda disponible como historico.
        entidad.setActivo(false);

        // Igual que en actualizar: la entidad esta gestionada, el dirty checking
        // se encarga del UPDATE al cerrar la transaccion.
    }

    /**
     * Recupera la entidad o corta el flujo con la excepcion de negocio.
     * Se centraliza aqui para no repetir el mismo mensaje en cada metodo.
     */
    private PartePlanta buscarOFallar(Long id) {
        return partePlantaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una parte de planta con id " + id));
    }
}
