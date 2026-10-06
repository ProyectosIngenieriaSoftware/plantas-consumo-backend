package sv.edu.ues.fmp.flora.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sv.edu.ues.fmp.flora.entity.Permiso;

@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);

    Optional<Permiso> findByCodigoIgnoreCase(String codigo);

    Page<Permiso> findByActivoTrue(Pageable pageable);
}
