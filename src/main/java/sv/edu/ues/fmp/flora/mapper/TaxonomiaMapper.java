package sv.edu.ues.fmp.flora.mapper;

import org.springframework.stereotype.Component;

import sv.edu.ues.fmp.flora.dto.request.TaxonomiaRequest;
import sv.edu.ues.fmp.flora.dto.response.TaxonomiaResponse;
import sv.edu.ues.fmp.flora.entity.Taxonomia;

/**
 * Convierte entre {@link TaxonomiaRequest}/{@link TaxonomiaResponse} y la
 * entidad {@link Taxonomia}. Escrito a mano, sin MapStruct, igual que el resto
 * de mappers del proyecto.
 */
@Component
public class TaxonomiaMapper {

    /** Unico valor admitido por el CHECK que compara {@code lower(reino)} con plantae. */
    private static final String REINO_POR_DEFECTO = "Plantae";

    public Taxonomia toEntity(TaxonomiaRequest request) {
        return Taxonomia.builder()
                .reino(resolverReino(request.getReino()))
                .division(request.getDivision())
                .clase(request.getClase())
                .ordenTaxonomico(request.getOrdenTaxonomico())
                .familia(request.getFamilia())
                .genero(request.getGenero())
                .especieTaxonomica(request.getEspecieTaxonomica())
                .subespecie(request.getSubespecie())
                .build();
    }

    public void updateEntity(Taxonomia entity, TaxonomiaRequest request) {
        entity.setReino(resolverReino(request.getReino()));
        entity.setDivision(request.getDivision());
        entity.setClase(request.getClase());
        entity.setOrdenTaxonomico(request.getOrdenTaxonomico());
        entity.setFamilia(request.getFamilia());
        entity.setGenero(request.getGenero());
        entity.setEspecieTaxonomica(request.getEspecieTaxonomica());
        entity.setSubespecie(request.getSubespecie());
    }

    public TaxonomiaResponse toResponse(Taxonomia entity) {
        return TaxonomiaResponse.builder()
                .idTaxonomia(entity.getIdTaxonomia())
                .reino(entity.getReino())
                .division(entity.getDivision())
                .clase(entity.getClase())
                .ordenTaxonomico(entity.getOrdenTaxonomico())
                .familia(entity.getFamilia())
                .genero(entity.getGenero())
                .especieTaxonomica(entity.getEspecieTaxonomica())
                .subespecie(entity.getSubespecie())
                .build();
    }

    /**
     * La columna es NOT NULL, asi que un reino ausente o en blanco se sustituye
     * por el valor por defecto en lugar de dejar que falle el INSERT.
     * <p>
     * Es publico a proposito: el servicio necesita el MISMO valor normalizado
     * para consultar {@code uk_taxonomia} antes de insertar. Si cada capa
     * aplicara su propia normalizacion, la validacion podria comparar contra un
     * reino distinto del que realmente se va a guardar y el duplicado se
     * escaparia hasta la base.
     */
    public String resolverReino(String reino) {
        return (reino == null || reino.isBlank()) ? REINO_POR_DEFECTO : reino;
    }
}
