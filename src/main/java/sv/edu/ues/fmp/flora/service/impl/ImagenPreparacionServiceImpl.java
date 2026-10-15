package sv.edu.ues.fmp.flora.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.dto.request.ImagenPreparacionRequest;
import sv.edu.ues.fmp.flora.dto.response.ImagenPreparacionResponse;
import sv.edu.ues.fmp.flora.entity.ImagenPreparacion;
import sv.edu.ues.fmp.flora.entity.PreparacionConsumo;
import sv.edu.ues.fmp.flora.exception.EstadoInvalidoException;
import sv.edu.ues.fmp.flora.exception.RecursoNoEncontradoException;
import sv.edu.ues.fmp.flora.mapper.ImagenPreparacionMapper;
import sv.edu.ues.fmp.flora.repository.ImagenPreparacionRepository;
import sv.edu.ues.fmp.flora.repository.PreparacionConsumoRepository;
import sv.edu.ues.fmp.flora.service.ImagenPreparacionService;

/**
 * Implementacion del servicio para la gestion de imagenes de preparaciones de consumo.
 * <p>
 * Reglas de negocio clave:
 * <ul>
 *   <li>Una preparacion puede tener como maximo una unica imagen principal activa.</li>
 *   <li>Al marcar o registrar una imagen como principal, la que estuviera previamente
 *       marcada se desmarca en la misma transaccion aplicando {@code flush()} para
 *       evitar conflictos con el indice unico parcial de PostgreSQL
 *       {@code uk_imagen_principal_preparacion}.</li>
 *   <li>Se valida la existencia y estado activo de la preparacion padre.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ImagenPreparacionServiceImpl implements ImagenPreparacionService {

    private final ImagenPreparacionRepository imagenPreparacionRepository;
    private final PreparacionConsumoRepository preparacionConsumoRepository;
    private final ImagenPreparacionMapper imagenPreparacionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ImagenPreparacionResponse> listarPorPreparacion(Long idPreparacion, boolean soloActivas) {
        verificarPreparacionExisteOFallar(idPreparacion);
        List<ImagenPreparacion> imagenes = soloActivas
                ? imagenPreparacionRepository.findByPreparacionIdPreparacionAndActivaTrueOrderByIdImagenPreparacionAsc(idPreparacion)
                : imagenPreparacionRepository.findByPreparacionIdPreparacionOrderByIdImagenPreparacionAsc(idPreparacion);
        return imagenes.stream().map(imagenPreparacionMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ImagenPreparacionResponse obtenerPorId(Long id) {
        return imagenPreparacionMapper.toResponse(buscarOFallar(id));
    }

    @Override
    public ImagenPreparacionResponse crear(Long idPreparacion, ImagenPreparacionRequest request) {
        PreparacionConsumo preparacion = buscarPreparacionOFallar(idPreparacion);
        verificarPreparacionActiva(preparacion);

        if (Boolean.TRUE.equals(request.getEsPrincipal())) {
            desmarcarPrincipalActual(idPreparacion);
        }

        ImagenPreparacion nueva = imagenPreparacionMapper.toEntity(request, preparacion);
        return imagenPreparacionMapper.toResponse(imagenPreparacionRepository.save(nueva));
    }

    @Override
    public ImagenPreparacionResponse actualizar(Long id, ImagenPreparacionRequest request) {
        ImagenPreparacion entidad = buscarOFallar(id);
        verificarPreparacionActiva(entidad.getPreparacion());
        Long idPreparacion = entidad.getPreparacion().getIdPreparacion();

        boolean promocion = Boolean.TRUE.equals(request.getEsPrincipal())
                && !Boolean.TRUE.equals(entidad.getEsPrincipal());
        if (promocion) {
            verificarImagenActiva(entidad);
            desmarcarPrincipalActual(idPreparacion);
        }

        imagenPreparacionMapper.updateEntity(entidad, request);
        return imagenPreparacionMapper.toResponse(entidad);
    }

    @Override
    public void desactivar(Long id) {
        ImagenPreparacion entidad = buscarOFallar(id);
        entidad.setActiva(false);
        entidad.setEsPrincipal(false);
    }

    @Override
    public ImagenPreparacionResponse activar(Long id) {
        ImagenPreparacion entidad = buscarOFallar(id);
        if (Boolean.TRUE.equals(entidad.getActiva())) {
            return imagenPreparacionMapper.toResponse(entidad);
        }

        verificarPreparacionActiva(entidad.getPreparacion());

        entidad.setEsPrincipal(false);
        entidad.setActiva(true);
        return imagenPreparacionMapper.toResponse(entidad);
    }

    @Override
    public ImagenPreparacionResponse marcarComoPrincipal(Long id) {
        ImagenPreparacion entidad = buscarOFallar(id);
        verificarImagenActiva(entidad);

        if (Boolean.TRUE.equals(entidad.getEsPrincipal())) {
            return imagenPreparacionMapper.toResponse(entidad);
        }

        verificarPreparacionActiva(entidad.getPreparacion());
        desmarcarPrincipalActual(entidad.getPreparacion().getIdPreparacion());
        entidad.setEsPrincipal(true);

        return imagenPreparacionMapper.toResponse(entidad);
    }

    /**
     * Desmarca la imagen principal activa actual para la preparacion especificada.
     * <p>
     * Aplica {@code flush()} para sincronizar el UPDATE en la base de datos antes de
     * ejecutar un nuevo INSERT o UPDATE que intente promover una imagen.
     * Esto evita violar el indice unico parcial {@code uk_imagen_principal_preparacion}
     * en PostgreSQL (comprobado sentencia a sentencia).
     */
    private void desmarcarPrincipalActual(Long idPreparacion) {
        imagenPreparacionRepository.findByPreparacionIdPreparacionAndEsPrincipalTrueAndActivaTrue(idPreparacion)
                .ifPresent(anterior -> {
                    anterior.setEsPrincipal(false);
                    imagenPreparacionRepository.flush();
                });
    }

    private ImagenPreparacion buscarOFallar(Long id) {
        return imagenPreparacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la imagen de preparación con id " + id));
    }

    private PreparacionConsumo buscarPreparacionOFallar(Long idPreparacion) {
        return preparacionConsumoRepository.findById(idPreparacion)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la preparación de consumo con id " + idPreparacion));
    }

    private void verificarPreparacionExisteOFallar(Long idPreparacion) {
        if (!preparacionConsumoRepository.existsById(idPreparacion)) {
            throw new RecursoNoEncontradoException(
                    "No existe la preparación de consumo con id " + idPreparacion);
        }
    }

    private void verificarPreparacionActiva(PreparacionConsumo preparacion) {
        if (!Boolean.TRUE.equals(preparacion.getActiva())) {
            throw new EstadoInvalidoException(
                    "La preparación '" + preparacion.getNombre()
                            + "' está desactivada; no se pueden gestionar sus imágenes.");
        }
    }

    private void verificarImagenActiva(ImagenPreparacion imagen) {
        if (!Boolean.TRUE.equals(imagen.getActiva())) {
            throw new EstadoInvalidoException(
                    "Una imagen desactivada no puede marcarse como principal.");
        }
    }
}

