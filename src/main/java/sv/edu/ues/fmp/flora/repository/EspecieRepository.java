package sv.edu.ues.fmp.flora.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sv.edu.ues.fmp.flora.entity.Especie;
import sv.edu.ues.fmp.flora.entity.enums.EstadoPublicacion;

/**
 * Acceso a datos de {@link Especie}.
 * <p>
 * Las consultas por nombre cientifico llevan sufijo {@code IgnoreCase} porque
 * la restriccion real de la base es el indice unico funcional
 * {@code uk_especie_nombre_cientifico_lower} sobre {@code lower(nombre_cientifico)}.
 * Recuerdese que la bandera de estado de esta tabla es {@code activa}, en femenino.
 */
@Repository
public interface EspecieRepository extends JpaRepository<Especie, Long> {

    /** Deteccion de duplicados antes de insertar, para devolver un 409 legible. */
    boolean existsByNombreCientificoIgnoreCase(String nombreCientifico);

    /** Permite comparar el id al actualizar y distinguir la misma especie de otra. */
    Optional<Especie> findByNombreCientificoIgnoreCase(String nombreCientifico);

    List<Especie> findByActivaTrue();

    List<Especie> findByEstadoPublicacion(EstadoPublicacion estado);

    /** Consulta del catalogo publico; se apoya en el indice {@code idx_especie_estado}. */
    List<Especie> findByEstadoPublicacionAndActivaTrue(EstadoPublicacion estado);
}
