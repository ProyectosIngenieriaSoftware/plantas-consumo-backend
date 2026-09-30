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

    // Permite al frontend buscar "qué plantas tienen Vitamina C", por ejemplo.
    List<AporteNutricional> findByNutrienteIdNutrienteOrderByIdAporteAsc(Long idNutriente);
}