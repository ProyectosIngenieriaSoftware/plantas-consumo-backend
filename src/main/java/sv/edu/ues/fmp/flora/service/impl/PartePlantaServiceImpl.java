package sv.edu.ues.fmp.flora.service.impl;

import java.util.ArrayList;
import java.util.List;

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
import sv.edu.ues.fmp.flora.util.Textos;

/**
 * Implementacion de la logica de negocio de las partes de planta.
 * Aqui viven las reglas (unicidad del nombre, baja y alta logica) y aqui
 * termina el recorrido de la entidad: hacia arriba solo salen DTOs.
 */
@Service
@RequiredArgsConstructor
public class PartePlantaServiceImpl implements PartePlantaService {

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
    public List<PartePlantaResponse> buscarPorNombre(String nombre) {
        // Un termino vacio devuelve la lista vacia y no el catalogo entero: el
        // LIKE con comodines a ambos lados casaria con todas las filas, que no
        // es lo que espera quien vacio la caja de busqueda.
        if (nombre == null || nombre.isBlank()) {
            return List.of();
        }

        List<PartePlantaResponse> respuestas = new ArrayList<>();
        for (PartePlanta entidad : partePlantaRepository.findByNombreContainingIgnoreCase(nombre.trim())) {
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
        // Se normaliza sobre el propio Request para que la consulta de
        // duplicados y el INSERT usen el mismo valor (ver Textos).
        request.setNombre(Textos.normalizar(request.getNombre()));

        // El nombre nunca se reutiliza, ni siquiera el de una parte
        // desactivada: para recuperarla esta PATCH /{id}/activar.
        partePlantaRepository.findByNombreIgnoreCase(request.getNombre())
                .ifPresent(existente -> lanzarDuplicado(existente, true));

        PartePlanta nueva = partePlantaMapper.toEntity(request);

        PartePlanta guardada = partePlantaRepository.save(nueva);

        return partePlantaMapper.toResponse(guardada);
    }

    @Override
    @Transactional
    public PartePlantaResponse actualizar(Long id, PartePlantaRequest request) {
        // Igual que en crear(): la consulta y el UPDATE deben ver el mismo valor.
        request.setNombre(Textos.normalizar(request.getNombre()));

        PartePlanta entidad = buscarOFallar(id);

        // Conservar el propio nombre no es un choque: el filter descarta la
        // coincidencia consigo misma.
        partePlantaRepository.findByNombreIgnoreCase(request.getNombre())
                .filter(otra -> !otra.getIdPartePlanta().equals(id))
                .ifPresent(otra -> lanzarDuplicado(otra, false));

        partePlantaMapper.updateEntity(entidad, request);

        return partePlantaMapper.toResponse(entidad);
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        PartePlanta entidad = buscarOFallar(id);

        entidad.setActivo(false);

    }

    @Override
    @Transactional
    public PartePlantaResponse activar(Long id) {
        PartePlanta entidad = buscarOFallar(id);

        // Idempotente: activar una que ya esta activa no es un error.
        if (!Boolean.TRUE.equals(entidad.getActivo())) {
            entidad.setActivo(true);
        }

        return partePlantaMapper.toResponse(entidad);
    }

    /**
     * Rechaza con 409 un nombre que ya usa otra parte de planta. El mensaje
     * distingue si esa parte esta desactivada, porque entonces el 409 no es un
     * callejon sin salida: el nombre se recupera reactivandola, y el mensaje
     * dice con que ruta.
     *
     * @param alCrear true desde crear(), donde el mensaje añade "en lugar de
     *                crear una nueva"; en actualizar() esa coletilla no aplica
     */
    private void lanzarDuplicado(PartePlanta existente, boolean alCrear) {
        if (Boolean.TRUE.equals(existente.getActivo())) {
            throw new RecursoDuplicadoException(
                    "Ya existe una parte de planta con el nombre '"
                            + existente.getNombre() + "'.");
        }
        throw new RecursoDuplicadoException(
                "Ya existe la parte de planta '" + existente.getNombre()
                        + "', pero está desactivada. Actívela con "
                        + "PATCH /api/partes-planta/" + existente.getIdPartePlanta()
                        + "/activar" + (alCrear ? " en lugar de crear una nueva." : "."));
    }

    private PartePlanta buscarOFallar(Long id) {
        return partePlantaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una parte de planta con id " + id));
    }
}
