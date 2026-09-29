package sv.edu.ues.fmp.flora.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sv.edu.ues.fmp.flora.entity.AporteNutricional;
import java.util.List;
import java.util.Optional;

@Repository
public interface AporteNutricionalRepository extends JpaRepository<AporteNutricional, Long> {

    // Evita duplicar el mismo nutriente para la misma parte de la planta
    boolean existsByIdEspecieParteAndNutrienteIdNutriente(Long idEspecieParte, Long idNutriente);

    Optional<AporteNutricional> findByIdEspecieParteAndNutrienteIdNutriente(Long idEspecieParte, Long idNutriente);

    // Lista todos los aportes de una parte de planta específica
    List<AporteNutricional> findByIdEspecieParteOrderByIdAporteAsc(Long idEspecieParte);
}