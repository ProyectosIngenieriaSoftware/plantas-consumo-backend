package sv.edu.ues.fmp.flora.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sv.edu.ues.fmp.flora.entity.AporteNutricional;
import java.util.List;
import java.util.Optional;

@Repository

public interface AporteNutricionalRepository extends JpaRepository<AporteNutricional, Long> {

    boolean existsByEspecieParteComestibleIdEspecieParteAndNutrienteIdNutriente(Long idEspecieParte, Long idNutriente);

    Optional<AporteNutricional> findByEspecieParteComestibleIdEspecieParteAndNutrienteIdNutriente(Long idEspecieParte, Long idNutriente);

    List<AporteNutricional> findByEspecieParteComestibleIdEspecieParteOrderByIdAporteAsc(Long idEspecieParte);

    /**
     * Los aportes vigentes de una parte comestible: los que no estan dados de
     * baja logica.
     * <p>
     * Es la consulta que usa el listado publico
     * {@code GET /api/aportes-nutricionales/especie-parte/{id}}, y lo hace
     * <strong>siempre</strong>, sin bandera que lo desactive: desde que
     * {@code DELETE /api/aportes-nutricionales/{id}} no borra la fila sino que
     * le pone {@code activo = false}, la version sin filtro seguiria pintando
     * los aportes retirados en la ficha publica. La hermana sin filtro se
     * queda para quien necesite ver el historial completo de la parte.
     */
    List<AporteNutricional> findByEspecieParteComestibleIdEspecieParteAndActivoTrueOrderByIdAporteAsc(Long idEspecieParte);

    List<AporteNutricional> findByNutrienteIdNutrienteOrderByIdAporteAsc(Long idNutriente);

    /**
     * Los aportes vigentes de un nutriente, para
     * {@code GET /api/aportes-nutricionales/nutriente/{id}?soloActivos=true}.
     * <p>
     * Aqui la bandera si es opcional, al contrario que en el listado por
     * parte: es un listado administrativo y por omision tiene que mostrar los
     * retirados, porque es desde donde se los reactiva. La misma convencion
     * que {@code GET /api/beneficios?soloActivos=true},
     * {@code GET /api/habitats?soloActivos=true} y
     * {@code GET /api/partes-planta?soloActivas=true}.
     */
    List<AporteNutricional> findByNutrienteIdNutrienteAndActivoTrueOrderByIdAporteAsc(Long idNutriente);
}