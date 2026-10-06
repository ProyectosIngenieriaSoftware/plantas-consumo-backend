package sv.edu.ues.fmp.flora.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import sv.edu.ues.fmp.flora.entity.ParteBeneficio;
import sv.edu.ues.fmp.flora.entity.enums.ParteBeneficioId;

public interface ParteBeneficioRepository extends JpaRepository<ParteBeneficio, ParteBeneficioId> {

    List<ParteBeneficio> findByActivaTrue();
}
