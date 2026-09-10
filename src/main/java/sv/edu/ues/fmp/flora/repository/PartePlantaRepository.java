package sv.edu.ues.fmp.flora.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sv.edu.ues.fmp.flora.entity.PartePlanta;


@Repository
public interface PartePlantaRepository extends JpaRepository<PartePlanta, Long> {

    /** Comprueba si ya hay una parte con ese nombre, sin distinguir mayusculas. */
    boolean existsByNombreIgnoreCase(String nombre);

    /** Recupera la parte con ese nombre para poder comparar su id al actualizar. */
    Optional<PartePlanta> findByNombreIgnoreCase(String nombre);

    /** Solo las partes vigentes, es decir las que no han sido dadas de baja logica. */
    List<PartePlanta> findByActivoTrue();
}
