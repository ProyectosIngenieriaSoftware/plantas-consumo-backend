package sv.edu.ues.fmp.flora.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sv.edu.ues.fmp.flora.entity.Fuente;

@Repository
public interface FuenteRepository extends JpaRepository<Fuente, Long> {

    boolean existsByTituloIgnoreCase(String titulo);

    Optional<Fuente> findByTituloIgnoreCase(String titulo);

    List<Fuente> findByActivaTrue();


    @Query("SELECT f FROM Fuente f WHERE f.activa = true AND LENGTH(f.titulo) > :min")
    List<Fuente> buscarActivasConTituloLargo(@Param("min") int min);

}
