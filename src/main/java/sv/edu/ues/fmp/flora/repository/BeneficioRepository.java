package sv.edu.ues.fmp.flora.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.ues.fmp.flora.entity.Beneficio;

import java.util.List;
import java.util.Optional;

public interface BeneficioRepository extends JpaRepository<Beneficio, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    Optional<Beneficio> findByNombreIgnoreCase(String nombre);

    /**
     * Solo los beneficios vigentes: los que no estan dados de baja logica.
     * <p>
     * El mismo metodo que ya tienen {@code HabitatRepository} y
     * {@code PartePlantaRepository}, y por la misma razon. Desde que
     * {@code DELETE /api/beneficios/{id}} no borra la fila sino que le pone
     * {@code activo = false}, el catalogo necesita una manera de pedirse sin
     * los de baja; la sirve {@code GET /api/beneficios?soloActivos=true}.
     * <p>
     * La tabla {@code beneficio} es un catalogo de pocas filas, asi que el
     * recorrido no necesita indice.
     */
    List<Beneficio> findByActivoTrue();
}