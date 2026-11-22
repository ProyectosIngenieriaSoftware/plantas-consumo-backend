package sv.edu.ues.fmp.flora.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import sv.edu.ues.fmp.flora.entity.ParteBeneficio;
import sv.edu.ues.fmp.flora.entity.enums.ParteBeneficioId;

public interface ParteBeneficioRepository extends JpaRepository<ParteBeneficio, ParteBeneficioId> {

    List<ParteBeneficio> findByActivaTrue();

    /**
     * Los beneficios activos de una sola parte comestible, para el listado por
     * padre que pinta la ficha publica.
     * <p>
     * Mismo criterio de estado que {@link #findByActivaTrue()}, el que sostiene
     * {@code GET /api/parte-beneficios/activas}: solo {@code activa = true}. El
     * orden por el id del beneficio es para que la respuesta no dependa del que
     * devuelva el motor.
     */
    List<ParteBeneficio>
    findByEspecieParteComestibleIdEspecieParteAndActivaTrueOrderByIdIdBeneficioAsc(
            Long idEspecieParte
    );
}
