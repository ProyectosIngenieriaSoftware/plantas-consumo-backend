package sv.edu.ues.fmp.flora.service.impl;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sv.edu.ues.fmp.flora.dto.request.ParteBeneficioRequest;
import sv.edu.ues.fmp.flora.dto.request.ParteBeneficioUpdateRequest;
import sv.edu.ues.fmp.flora.dto.response.ParteBeneficioResponse;
import sv.edu.ues.fmp.flora.entity.Beneficio;
import sv.edu.ues.fmp.flora.entity.EspecieParteComestible;
import sv.edu.ues.fmp.flora.entity.Fuente;
import sv.edu.ues.fmp.flora.entity.ParteBeneficio;
import sv.edu.ues.fmp.flora.entity.enums.ParteBeneficioId;
import sv.edu.ues.fmp.flora.exception.EstadoInvalidoException;
import sv.edu.ues.fmp.flora.exception.IdInvalidoException;
import sv.edu.ues.fmp.flora.exception.RecursoDuplicadoException;
import sv.edu.ues.fmp.flora.exception.RecursoNoEncontradoException;
import sv.edu.ues.fmp.flora.mapper.ParteBeneficioMapper;
import sv.edu.ues.fmp.flora.repository.BeneficioRepository;
import sv.edu.ues.fmp.flora.repository.EspecieParteComestibleRepository;
import sv.edu.ues.fmp.flora.repository.FuenteRepository;
import sv.edu.ues.fmp.flora.repository.ParteBeneficioRepository;
import sv.edu.ues.fmp.flora.service.ParteBeneficioService;

@Service
@RequiredArgsConstructor
public class ParteBeneficioServiceImpl implements ParteBeneficioService {

    private final ParteBeneficioRepository parteBeneficioRepository;
    private final BeneficioRepository beneficioRepository;
    private final EspecieParteComestibleRepository especieParteComestibleRepository;
    private final FuenteRepository fuenteRepository;
    private final ParteBeneficioMapper parteBeneficioMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ParteBeneficioResponse> listarTodas() {
        return parteBeneficioRepository.findAll()
                .stream()
                .map(parteBeneficioMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParteBeneficioResponse> listarActivas() {
        return parteBeneficioRepository.findByActivaTrue()
                .stream()
                .map(parteBeneficioMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ParteBeneficioResponse obtenerPorId(
            Long idEspecieParte,
            Long idBeneficio
    ) {
        ParteBeneficio entidad = buscarOFallar(
                idEspecieParte,
                idBeneficio
        );

        return parteBeneficioMapper.toResponse(entidad);
    }

    @Override
    @Transactional
    public ParteBeneficioResponse crear(ParteBeneficioRequest request) {
        ParteBeneficioId id = validarClave(
                request.getIdEspecieParte(),
                request.getIdBeneficio()
        );

        if (parteBeneficioRepository.existsById(id)) {
            throw duplicado(id);
        }

        EspecieParteComestible parteComestible = buscarParteComestible(
                id.getIdEspecieParte()
        );

        Beneficio beneficio = buscarBeneficio(
                id.getIdBeneficio()
        );

        Fuente fuente = buscarFuente(request.getIdFuente());

        validarRelacionesActivas(
                id.getIdEspecieParte(),
                parteComestible.getActiva(),
                beneficio,
                fuente
        );

        ParteBeneficio nueva = parteBeneficioMapper.toEntity(
                request,
                parteComestible,
                beneficio,
                fuente
        );

        ParteBeneficio guardada = parteBeneficioRepository.save(nueva);

        return parteBeneficioMapper.toResponse(guardada);
    }

    @Override
    @Transactional
    public ParteBeneficioResponse actualizar(
            Long idEspecieParte,
            Long idBeneficio,
            ParteBeneficioUpdateRequest request
    ) {
        ParteBeneficio entidad = buscarOFallar(
                idEspecieParte,
                idBeneficio
        );

        // La fuente siempre debe existir, aunque la asociación quede inactiva.
        Fuente fuente = buscarFuente(request.getIdFuente());

        // Si no se envía activa, se conserva el estado actual.
        boolean quedaraActiva = request.getActiva() != null
                ? request.getActiva()
                : Boolean.TRUE.equals(entidad.getActiva());

        /*
         * Se comprueban las relaciones al reactivar y también al editar
         * una asociación que seguirá activa.
         */
        if (quedaraActiva) {
            EspecieParteComestible parteComestible = buscarParteComestible(idEspecieParte);
            Beneficio beneficio = buscarBeneficio(idBeneficio);

            validarRelacionesActivas(
                    idEspecieParte,
                    parteComestible.getActiva(),
                    beneficio,
                    fuente
            );
        }

        parteBeneficioMapper.updateEntity(
                entidad,
                request,
                fuente
        );

        return parteBeneficioMapper.toResponse(entidad);
    }

    @Override
    @Transactional
    public void desactivar(Long idEspecieParte, Long idBeneficio) {
        // Dar de baja sigue permitido aunque sus relaciones estén inactivas.
        buscarOFallar(idEspecieParte, idBeneficio).setActiva(false);
    }

    private void validarRelacionesActivas(
            Long idEspecieParte,
            Boolean parteActiva,
            Beneficio beneficio,
            Fuente fuente
    ) {
        if (!Boolean.TRUE.equals(parteActiva)) {
            throw new EstadoInvalidoException(
                    "La parte comestible con id "
                            + idEspecieParte
                            + " está inactiva. No se puede guardar "
                            + "una asociación activa."
            );
        }

        if (!Boolean.TRUE.equals(beneficio.getActivo())) {
            throw new EstadoInvalidoException(
                    "El beneficio con id "
                            + beneficio.getIdBeneficio()
                            + " está inactivo. No se puede guardar "
                            + "una asociación activa."
            );
        }

        if (!Boolean.TRUE.equals(fuente.getActiva())) {
            throw new EstadoInvalidoException(
                    "La fuente con id "
                            + fuente.getIdFuente()
                            + " está inactiva. No se puede guardar "
                            + "una asociación activa."
            );
        }
    }

    private EspecieParteComestible buscarParteComestible(Long idEspecieParte) {
        return especieParteComestibleRepository.findById(idEspecieParte)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la parte comestible con id "
                                + idEspecieParte
                ));
    }

    private Beneficio buscarBeneficio(Long idBeneficio) {
        return beneficioRepository.findById(idBeneficio)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el beneficio con id " + idBeneficio
                ));
    }

    private Fuente buscarFuente(Long idFuente) {
        validarId(idFuente, "fuente");

        return fuenteRepository.findById(idFuente)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la fuente con id " + idFuente
                ));
    }

    private ParteBeneficio buscarOFallar(
            Long idEspecieParte,
            Long idBeneficio
    ) {
        ParteBeneficioId id = validarClave(
                idEspecieParte,
                idBeneficio
        );

        return parteBeneficioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la asociación entre la parte comestible "
                                + idEspecieParte
                                + " y el beneficio "
                                + idBeneficio
                ));
    }

    private ParteBeneficioId validarClave(
            Long idEspecieParte,
            Long idBeneficio
    ) {
        validarId(idEspecieParte, "parte comestible");
        validarId(idBeneficio, "beneficio");

        return new ParteBeneficioId(idEspecieParte, idBeneficio);
    }

    private void validarId(Long id, String recurso) {
        if (id == null || id <= 0) {
            throw new IdInvalidoException(
                    "El id de " + recurso + " debe ser un valor positivo"
            );
        }
    }

    private RecursoDuplicadoException duplicado(ParteBeneficioId id) {
        return new RecursoDuplicadoException(
                "Ya existe la asociación entre la parte comestible "
                        + id.getIdEspecieParte()
                        + " y el beneficio "
                        + id.getIdBeneficio()
                        + ", incluso si está inactiva"
        );
    }
}
