package sv.edu.ues.fmp.flora.mapper;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.dto.request.EspecieRequest;
import sv.edu.ues.fmp.flora.dto.response.EspecieResponse;
import sv.edu.ues.fmp.flora.entity.Especie;
import sv.edu.ues.fmp.flora.entity.Taxonomia;
import sv.edu.ues.fmp.flora.entity.Usuario;

/**
 * Convierte entre {@link EspecieRequest}/{@link EspecieResponse} y la entidad
 * {@link Especie}.
 * <p>
 * El mapper no consulta repositorios: recibe del servicio las entidades ya
 * resueltas ({@link Taxonomia}, {@link Usuario}) y se limita a armar objetos.
 */
@Component
@RequiredArgsConstructor
public class EspecieMapper {

    private final TaxonomiaMapper taxonomiaMapper;

    /**
     * Construye la especie a partir de entidades ya validadas por el servicio.
     * No asigna estado ni fechas: el estado lo pone el {@code @Builder.Default}
     * en BORRADOR y las fechas las escribe PostgreSQL.
     */
    public Especie toEntity(EspecieRequest request, Taxonomia taxonomia, Usuario creador) {
        return Especie.builder()
                .taxonomia(taxonomia)
                .nombreCientifico(request.getNombreCientifico())
                .descripcion(request.getDescripcion())
                .origen(request.getOrigen())
                .propiedades(request.getPropiedades())
                .importanciaCultural(request.getImportanciaCultural())
                .advertencias(request.getAdvertencias())
                .creadaPor(creador)
                .build();
    }

    /**
     * Actualiza solo los campos que el usuario puede editar.
     * Deliberadamente NO toca estado, fechas ni usuarios de validacion o
     * publicacion: eso solo cambia por las transiciones de estado del servicio.
     */
    public void updateEntity(Especie entity, EspecieRequest request) {
        entity.setNombreCientifico(request.getNombreCientifico());
        entity.setDescripcion(request.getDescripcion());
        entity.setOrigen(request.getOrigen());
        entity.setPropiedades(request.getPropiedades());
        entity.setImportanciaCultural(request.getImportanciaCultural());
        entity.setAdvertencias(request.getAdvertencias());
    }

    public EspecieResponse toResponse(Especie entity) {
        return EspecieResponse.builder()
                .idEspecie(entity.getIdEspecie())
                .nombreCientifico(entity.getNombreCientifico())
                .descripcion(entity.getDescripcion())
                .origen(entity.getOrigen())
                .propiedades(entity.getPropiedades())
                .importanciaCultural(entity.getImportanciaCultural())
                .advertencias(entity.getAdvertencias())
                .estadoPublicacion(entity.getEstadoPublicacion())
                .activa(entity.getActiva())
                .taxonomia(taxonomiaMapper.toResponse(entity.getTaxonomia()))
                .creadaPor(nombreCompleto(entity.getCreadaPor()))
                .validadaPor(nombreCompleto(entity.getValidadaPor()))
                .publicadaPor(nombreCompleto(entity.getPublicadaPor()))
                .fechaRegistro(entity.getFechaRegistro())
                .fechaActualizacion(entity.getFechaActualizacion())
                .fechaValidacion(entity.getFechaValidacion())
                .fechaPublicacion(entity.getFechaPublicacion())
                .build();
    }

    /**
     * {@code validadaPor} y {@code publicadaPor} son nulos mientras la ficha no
     * pase por esas etapas, asi que el nombre sale como null en vez de reventar
     * con NullPointerException.
     */
    private String nombreCompleto(Usuario usuario) {
        return usuario == null ? null : usuario.getNombres() + " " + usuario.getApellidos();
    }
}
