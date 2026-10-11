package sv.edu.ues.fmp.flora.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.dto.request.ImagenEspecieRequest;
import sv.edu.ues.fmp.flora.dto.response.ImagenEspecieResponse;
import sv.edu.ues.fmp.flora.entity.*;
import sv.edu.ues.fmp.flora.exception.EstadoInvalidoException;
import sv.edu.ues.fmp.flora.exception.RecursoNoEncontradoException;
import sv.edu.ues.fmp.flora.mapper.ImagenEspecieMapper;
import sv.edu.ues.fmp.flora.repository.EspecieRepository;
import sv.edu.ues.fmp.flora.repository.ImagenEspecieRepository;
import sv.edu.ues.fmp.flora.service.ImagenEspecieService;

/** Gestiona imágenes, valida sus relaciones y coordina el índice parcial de principal activa. */
@Service
@RequiredArgsConstructor
@Transactional
public class ImagenEspecieServiceImpl implements ImagenEspecieService {
    private final ImagenEspecieRepository imagenEspecieRepository;
    private final EspecieRepository especieRepository;
    private final ValidadorParteEspecie validadorParteEspecie;
    private final ImagenEspecieMapper imagenEspecieMapper;

    /** Distingue especie inexistente de un listado vacío; permite consultar el historial. */
    @Override
    @Transactional(readOnly = true)
    public List<ImagenEspecieResponse> listarPorEspecie(Long idEspecie, boolean soloActivas) {
        if (!especieRepository.existsById(idEspecie)) {
            throw new RecursoNoEncontradoException("No existe la especie con id " + idEspecie);
        }
        List<ImagenEspecie> imagenes = soloActivas
                ? imagenEspecieRepository.findByEspecieIdEspecieAndActivaTrueOrderByIdImagenAsc(idEspecie)
                : imagenEspecieRepository.findByEspecieIdEspecieOrderByIdImagenAsc(idEspecie);
        return imagenes.stream().map(imagenEspecieMapper::toResponse).toList();
    }

    /** Consulta una imagen sin filtrar su estado. */
    @Override
    @Transactional(readOnly = true)
    public ImagenEspecieResponse obtenerPorId(Long id) {
        return imagenEspecieMapper.toResponse(buscarOFallar(id));
    }

    /** Valida especie y parte antes de desplazar a la principal e insertar. */
    @Override
    public ImagenEspecieResponse crear(Long idEspecie, ImagenEspecieRequest request) {
        Especie especie = especieRepository.findById(idEspecie)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la especie con id " + idEspecie));
        verificarEspecieActiva(especie);
        EspecieParteComestible parte = validadorParteEspecie.resolver(idEspecie, request.getIdEspecieParte());
        if (Boolean.TRUE.equals(request.getEsPrincipal())) {
            desmarcarPrincipalActual(idEspecie);
        }
        ImagenEspecie nueva = imagenEspecieMapper.toEntity(request, especie, parte);
        return imagenEspecieMapper.toResponse(imagenEspecieRepository.save(nueva));
    }

    /** Solo desplaza a la principal en la transición false a true. */
    @Override
    public ImagenEspecieResponse actualizar(Long id, ImagenEspecieRequest request) {
        ImagenEspecie entidad = buscarOFallar(id);
        verificarEspecieActiva(entidad.getEspecie());
        Long idEspecie = entidad.getEspecie().getIdEspecie();
        EspecieParteComestible parte = validadorParteEspecie.resolver(idEspecie, request.getIdEspecieParte());
        boolean promocion = Boolean.TRUE.equals(request.getEsPrincipal())
                && !Boolean.TRUE.equals(entidad.getEsPrincipal());
        if (promocion) {
            verificarImagenActiva(entidad);
            desmarcarPrincipalActual(idEspecie);
        }
        // Entidad gestionada: Hibernate emite el UPDATE sin llamar a save().
        imagenEspecieMapper.updateEntity(entidad, request, parte);
        return imagenEspecieMapper.toResponse(entidad);
    }

    /** Baja idempotente; se permite desactivar la principal y conservar su bandera. */
    @Override
    public void desactivar(Long id) {
        buscarOFallar(id).setActiva(false);
    }

    /** Reactiva de forma idempotente, cediendo el puesto a la principal que regresa. */
    @Override
    public ImagenEspecieResponse activar(Long id) {
        ImagenEspecie entidad = buscarOFallar(id);
        if (Boolean.TRUE.equals(entidad.getActiva())) {
            return imagenEspecieMapper.toResponse(entidad);
        }
        verificarEspecieActiva(entidad.getEspecie());
        validadorParteEspecie.resolver(entidad.getEspecie().getIdEspecie(),
                entidad.getEspecieParte() == null ? null : entidad.getEspecieParte().getIdEspecieParte());
        if (Boolean.TRUE.equals(entidad.getEsPrincipal())) {
            desmarcarPrincipalActual(entidad.getEspecie().getIdEspecie());
        }
        entidad.setActiva(true);
        return imagenEspecieMapper.toResponse(entidad);
    }

    /** Repetir la promoción no cambia la principal ni la desmarca a sí misma. */
    @Override
    public ImagenEspecieResponse marcarComoPrincipal(Long id) {
        ImagenEspecie entidad = buscarOFallar(id);
        verificarImagenActiva(entidad);
        if (Boolean.TRUE.equals(entidad.getEsPrincipal())) {
            return imagenEspecieMapper.toResponse(entidad);
        }
        verificarEspecieActiva(entidad.getEspecie());
        desmarcarPrincipalActual(entidad.getEspecie().getIdEspecie());
        entidad.setEsPrincipal(true);
        return imagenEspecieMapper.toResponse(entidad);
    }

    /** Libera el índice parcial antes del INSERT o UPDATE que promueve otra imagen. */
    private void desmarcarPrincipalActual(Long idEspecie) {
        imagenEspecieRepository.findByEspecieIdEspecieAndEsPrincipalTrueAndActivaTrue(idEspecie)
                .ifPresent(anterior -> {
                    anterior.setEsPrincipal(false);
                    // No se llama a save(): anterior es una entidad gestionada.
                    // Hibernate pone los INSERT antes de los UPDATE y tampoco garantiza
                    // el orden entre dos UPDATE. El índice parcial no es DEFERRABLE:
                    // PostgreSQL lo comprueba sentencia a sentencia. flush() adelanta
                    // la desmarcación antes de guardar o promover la nueva principal.
                    imagenEspecieRepository.flush();
                });
    }

    /** Resuelve el registro o devuelve el 404 de negocio. */
    private ImagenEspecie buscarOFallar(Long id) {
        return imagenEspecieRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la imagen con id " + id));
    }

    /** Las nuevas asociaciones y modificaciones requieren una especie activa. */
    private void verificarEspecieActiva(Especie especie) {
        if (!Boolean.TRUE.equals(especie.getActiva())) {
            throw new EstadoInvalidoException("La especie '" + especie.getNombreCientifico()
                    + "' está desactivada; no se pueden registrar o promover sus imágenes.");
        }
    }

    /** Una imagen desactivada conserva su historial, pero no puede promocionarse. */
    private void verificarImagenActiva(ImagenEspecie imagen) {
        if (!Boolean.TRUE.equals(imagen.getActiva())) {
            throw new EstadoInvalidoException("Una imagen desactivada no puede marcarse como principal.");
        }
    }
}
