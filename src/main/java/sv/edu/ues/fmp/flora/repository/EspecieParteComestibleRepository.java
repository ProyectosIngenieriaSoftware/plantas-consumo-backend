package sv.edu.ues.fmp.flora.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sv.edu.ues.fmp.flora.entity.EspecieParteComestible;

/**
 * Acceso a datos de {@link EspecieParteComestible}.
 * <p>
 * Los listados arrancan por la especie y se apoyan en el indice
 * {@code idx_parte_especie}. Recuerdese que la bandera de estado de esta tabla
 * es {@code activa}, en femenino.
 */
@Repository
public interface EspecieParteComestibleRepository extends JpaRepository<EspecieParteComestible, Long> {

    /** Todas las partes de la especie, activas e inactivas, por nombre de parte. */
    List<EspecieParteComestible> findByEspecieIdEspecieOrderByPartePlantaNombreAsc(Long idEspecie);

    List<EspecieParteComestible> findByEspecieIdEspecieAndActivaTrueOrderByPartePlantaNombreAsc(Long idEspecie);

    /**
     * Deteccion de duplicados antes de insertar. Devuelve como maximo una fila
     * porque asi lo garantiza {@code uk_especie_parte}, que no es parcial: la
     * busqueda incluye registros desactivados, y el servicio usa la bandera del
     * encontrado para elegir el mensaje.
     */
    Optional<EspecieParteComestible> findByEspecieIdEspecieAndPartePlantaIdPartePlanta(
            Long idEspecie, Long idPartePlanta);
}
