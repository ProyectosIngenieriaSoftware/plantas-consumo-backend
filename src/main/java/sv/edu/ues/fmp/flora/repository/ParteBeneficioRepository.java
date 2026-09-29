package sv.edu.ues.fmp.flora.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import sv.edu.ues.fmp.flora.entity.ParteBeneficio;
import sv.edu.ues.fmp.flora.entity.enums.ParteBeneficioId;

public interface ParteBeneficioRepository extends JpaRepository<ParteBeneficio, ParteBeneficioId> {

    List<ParteBeneficio> findByActivaTrue();

    // SQL nativo por que mientras falta EspecieParteComestible.
    @Query(value = """
            SELECT activa
            FROM public.especie_parte_comestible
            WHERE id_especie_parte = :id
            """, nativeQuery = true)


    Optional<Boolean> buscarEstadoParteComestible(@Param("id") Long id);
}