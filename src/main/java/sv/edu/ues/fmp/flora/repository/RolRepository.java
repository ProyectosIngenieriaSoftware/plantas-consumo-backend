package sv.edu.ues.fmp.flora.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sv.edu.ues.fmp.flora.entity.Rol;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    Optional<Rol> findByNombreIgnoreCase(String nombre);

    Page<Rol> findByActivoTrue(Pageable pageable);
}
