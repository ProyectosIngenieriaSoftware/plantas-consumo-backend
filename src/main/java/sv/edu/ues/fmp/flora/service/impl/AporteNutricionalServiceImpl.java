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
import sv.edu.ues.fmp.flora.service.VisibilidadDeEspecie;

@Service
@RequiredArgsConstructor
public class AporteNutricionalServiceImpl implements AporteNutricionalService {

    private final AporteNutricionalRepository aporteRepository;
    private final EspecieParteComestibleRepository especieParteRepository;
    private final NutrienteRepository nutrienteRepository;
    private final FuenteRepository fuenteRepository;
    private final AporteNutricionalMapper mapper;

    /**
     * Decide si el contenido de la especie padre se puede mostrar a quien
     * llama. Lo que no se puede ver se responde como lo que no existe.
     */
    private final VisibilidadDeEspecie visibilidadDeEspecie;

    @Override
    @Transactional(readOnly = true)
    public List<AporteNutricionalResponse> listarPorEspecieParte(Long idEspecieParte) {
        // Mismo patron que sus hermanos (epocas de cosecha, preparaciones y
        // parte-beneficios): la parte que no existe y la parte de una ficha que
        // quien llama no puede ver responden el mismo 404, con el mismo texto.
        // Una parte visible sin aportes sigue devolviendo 200 con lista vacia.
        EspecieParteComestible parte = especieParteRepository.findById(idEspecieParte)
                .orElseThrow(() -> parteNoEncontrada(idEspecieParte));

        if (!visibilidadDeEspecie.puedeVer(parte.getEspecie().getIdEspecie())) {
            throw parteNoEncontrada(idEspecieParte);
        }

        // Al filtro de visibilidad del padre se suma el de la baja logica del
        // propio aporte, y este no tiene bandera que lo apague: un aporte
        // retirado no se pinta en la ficha publica. Son dos preguntas
        // distintas --si la ficha se puede ver, y si el renglon sigue
        // vigente-- y las dos tienen que dar si.
        //
        // Una parte visible con todos sus aportes retirados sigue siendo un
        // 200 con lista vacia, no un 404: la parte existe y se puede ver, es
        // su contenido el que esta vacio. El 404 de arriba habla de la parte,
        // y confundirlos delataria por omision que esa parte tuvo aportes.
        return aporteRepository
                .findByEspecieParteComestibleIdEspecieParteAndActivoTrueOrderByIdAporteAsc(idEspecieParte)
                .stream().map(mapper::toResponse).toList();
    }

    /**
     * El 404 de la parte comestible, en un solo sitio: lo lanzan la parte que
     * no existe y la parte de una ficha que quien llama no puede ver. Si los
     * mensajes difirieran, delatarian que ese id es de una ficha sin publicar.
     * Es el mismo texto que usan los servicios hermanos.
     */
    private static RecursoNoEncontradoException parteNoEncontrada(Long idEspecieParte) {
        return new RecursoNoEncontradoException("No existe la parte comestible con id " + idEspecieParte);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AporteNutricionalResponse> listarPorNutriente(Long idNutriente) {
        return aporteRepository.findByNutrienteIdNutrienteOrderByIdAporteAsc(idNutriente)
                .stream().map(mapper::toResponse).toList();
    }

    /**
     * El mismo listado administrativo, sin los dados de baja. Es la cara de
     * {@code ?soloActivos=true}; sin la bandera sigue respondiendo
     * {@link #listarPorNutriente(Long)}, con los retirados incluidos, porque
     * es desde esta pantalla desde donde se los reactiva.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AporteNutricionalResponse> listarActivosPorNutriente(Long idNutriente) {
        return aporteRepository.findByNutrienteIdNutrienteAndActivoTrueOrderByIdAporteAsc(idNutriente)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AporteNutricionalResponse obtenerPorId(Long id) {
        AporteNutricional entidad = buscarOFallar(id);

        if (!visibilidadDeEspecie.puedeVer(
                entidad.getEspecieParteComestible().getEspecie().getIdEspecie())) {

            throw noEncontrado(id);
        }

        return mapper.toResponse(entidad);
    }

    /** El 404 del recurso, en un solo sitio para que las dos salidas coincidan. */
    private static RecursoNoEncontradoException noEncontrado(Long id) {
        return new RecursoNoEncontradoException("No existe un aporte nutricional con id " + id);
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

        Fuente fuente = null;
        if (request.getIdFuente() != null) {
            fuente = obtenerFuenteOFallar(request.getIdFuente());
        }

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

        Fuente fuente = null;
        if (request.getIdFuente() != null) {
            fuente = obtenerFuenteOFallar(request.getIdFuente());
        }

        mapper.updateEntity(entidad, request, especieParte, nutriente, fuente);
        return mapper.toResponse(entidad);
    }

    /**
     * Baja logica, no borrado. Antes aqui habia un {@code delete()} y el aporte
     * desaparecia de la base: se perdia una medicion citada a una fuente y un
     * borrado por error solo se arreglaba volviendo a teclearla. Ahora la fila
     * se queda con {@code activo = false}; lo que cambia es que deja de
     * pintarse en la ficha publica.
     * <p>
     * Es el patron del resto del catalogo: igual que
     * {@code DELETE /api/beneficios/{id}}, {@code /api/habitats/{id}} y
     * {@code /api/partes-planta/{id}}. Se revierte con {@link #activar(Long)}.
     */
    @Override
    @Transactional
    public void desactivar(Long id) {
        AporteNutricional entidad = buscarOFallar(id);
        entidad.setActivo(false);
        aporteRepository.save(entidad);
    }

    /**
     * Devuelve al catalogo un aporte retirado.
     * <p>
     * Idempotente, como sus hermanos: activar uno que ya esta activo no es un
     * error, no cambia nada y responde igual.
     * <p>
     * No pasa por {@code VisibilidadDeEspecie}, al contrario que
     * {@link #obtenerPorId(Long)}: es una operacion de escritura, protegida
     * por {@code NUTRICION_GESTIONAR}, y quien gestiona el contenido trabaja
     * precisamente sobre fichas que no estan publicadas todavia.
     */
    @Override
    @Transactional
    public AporteNutricionalResponse activar(Long id) {
        AporteNutricional entidad = buscarOFallar(id);

        if (!Boolean.TRUE.equals(entidad.getActivo())) {
            entidad.setActivo(true);
            aporteRepository.save(entidad);
        }

        return mapper.toResponse(entidad);
    }

    private AporteNutricional buscarOFallar(Long id) {
        return aporteRepository.findById(id)
                .orElseThrow(() -> noEncontrado(id));
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
        Nutriente nutriente = nutrienteRepository.findById(idNutriente)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el nutriente con id " + idNutriente));
        if (!Boolean.TRUE.equals(nutriente.getActivo())) {
            throw new EstadoInvalidoException("El nutriente seleccionado se encuentra desactivado.");
        }
        return nutriente;
    }

    private Fuente obtenerFuenteOFallar(Long idFuente) {
        Fuente fuente = fuenteRepository.findById(idFuente)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la fuente con id " + idFuente));
        if (!Boolean.TRUE.equals(fuente.getActiva())) {
            throw new EstadoInvalidoException("La fuente seleccionada se encuentra desactivada.");
        }
        return fuente;
    }
}
