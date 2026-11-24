package sv.edu.ues.fmp.flora.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sv.edu.ues.fmp.flora.entity.Habitat;

@Repository
public interface HabitatRepository extends JpaRepository<Habitat, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    Optional<Habitat> findByNombreIgnoreCase(String nombre);

    List<Habitat> findByNombreContainingIgnoreCase(String nombre);

    List<Habitat> findByActivoTrue();
}