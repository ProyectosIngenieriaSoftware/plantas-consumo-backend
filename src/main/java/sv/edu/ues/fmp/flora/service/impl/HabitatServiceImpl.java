package sv.edu.ues.fmp.flora.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.dto.request.HabitatRequest;
import sv.edu.ues.fmp.flora.dto.response.HabitatResponse;
import sv.edu.ues.fmp.flora.entity.Habitat;
import sv.edu.ues.fmp.flora.exception.RecursoDuplicadoException;
import sv.edu.ues.fmp.flora.exception.RecursoNoEncontradoException;
import sv.edu.ues.fmp.flora.mapper.HabitatMapper;
import sv.edu.ues.fmp.flora.repository.HabitatRepository;
import sv.edu.ues.fmp.flora.service.HabitatService;
import sv.edu.ues.fmp.flora.util.Textos;

@Service
@RequiredArgsConstructor
public class HabitatServiceImpl implements HabitatService {

    private final HabitatRepository habitatRepository;
    private final HabitatMapper habitatMapper;

    @Override
    @Transactional(readOnly = true)
    public List<HabitatResponse> listarTodos() {
        List<HabitatResponse> respuestas = new ArrayList<>();
        for (Habitat entidad : habitatRepository.findAll()) {
            respuestas.add(habitatMapper.toResponse(entidad));
        }
        return respuestas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HabitatResponse> listarActivos() {
        List<HabitatResponse> respuestas = new ArrayList<>();
        for (Habitat entidad : habitatRepository.findByActivoTrue()) {
            respuestas.add(habitatMapper.toResponse(entidad));
        }
        return respuestas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HabitatResponse> buscarPorNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El parámetro de búsqueda 'nombre' es obligatorio y no puede estar vacío"
            );
        }

        List<HabitatResponse> respuestas = new ArrayList<>();
        for (Habitat entidad : habitatRepository.findByNombreContainingIgnoreCase(nombre.trim())) {
            respuestas.add(habitatMapper.toResponse(entidad));
        }
        return respuestas;
    }

    @Override
    @Transactional(readOnly = true)
    public HabitatResponse obtenerPorId(Long id) {
        return habitatMapper.toResponse(buscarOFallar(id));
    }

    @Override
    @Transactional
    public HabitatResponse crear(HabitatRequest request) {
        // Se normaliza sobre el propio Request para que la consulta de
        // duplicados y el INSERT usen el mismo valor (ver Textos).
        request.setNombre(Textos.normalizar(request.getNombre()));

        // El nombre nunca se reutiliza, ni siquiera el de un hábitat
        // desactivado: para recuperarlo está PATCH /{id}/activar.
        habitatRepository.findByNombreIgnoreCase(request.getNombre())
                .ifPresent(existente -> lanzarDuplicado(existente, true));

        Habitat nuevo = habitatMapper.toEntity(request);
        if (nuevo.getActivo() == null) {
            nuevo.setActivo(true);
        }
        Habitat guardado = habitatRepository.save(nuevo);

        return habitatMapper.toResponse(guardado);
    }

    @Override
    @Transactional
    public HabitatResponse actualizar(Long id, HabitatRequest request) {
        // Igual que en crear(): la consulta y el UPDATE deben ver el mismo valor.
        request.setNombre(Textos.normalizar(request.getNombre()));

        Habitat entidad = buscarOFallar(id);

        // Conservar el propio nombre no es un choque: el filter descarta la
        // coincidencia consigo mismo.
        habitatRepository.findByNombreIgnoreCase(request.getNombre())
                .filter(otro -> !otro.getIdHabitat().equals(id))
                .ifPresent(otro -> lanzarDuplicado(otro, false));

        habitatMapper.updateEntity(entidad, request);
        return habitatMapper.toResponse(entidad);
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Habitat entidad = buscarOFallar(id);
        entidad.setActivo(false);
    }

    @Override
    @Transactional
    public HabitatResponse activar(Long id) {
        Habitat entidad = buscarOFallar(id);

        // Idempotente: activar uno que ya está activo no es un error.
        if (!Boolean.TRUE.equals(entidad.getActivo())) {
            entidad.setActivo(true);
        }

        return habitatMapper.toResponse(entidad);
    }

    /**
     * Rechaza con 409 un nombre que ya usa otro hábitat. El mensaje distingue
     * si ese hábitat está desactivado, para indicar cómo recuperarlo.
     *
     * @param alCrear true desde crear(), donde el mensaje añade "en lugar de
     *                crear uno nuevo"; en actualizar() esa coletilla no aplica
     */
    private void lanzarDuplicado(Habitat existente, boolean alCrear) {
        if (Boolean.TRUE.equals(existente.getActivo())) {
            throw new RecursoDuplicadoException(
                    "Ya existe un hábitat con el nombre '" + existente.getNombre() + "'.");
        }
        throw new RecursoDuplicadoException(
                "Ya existe el hábitat '" + existente.getNombre() + "', pero está desactivado. "
                        + "Actívelo con PATCH /api/habitats/" + existente.getIdHabitat()
                        + "/activar" + (alCrear ? " en lugar de crear uno nuevo." : "."));
    }

    private Habitat buscarOFallar(Long id) {
        return habitatRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un hábitat con id " + id));
    }
}