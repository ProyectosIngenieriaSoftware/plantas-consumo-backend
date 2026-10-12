package sv.edu.ues.fmp.flora.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sv.edu.ues.fmp.flora.entity.UbicacionEspecie;
import java.util.List;

@Repository
public interface UbicacionEspecieRepository extends JpaRepository<UbicacionEspecie, Long> {

    List<UbicacionEspecie> findByEspecie_IdEspecieAndActivaTrue(Long idEspecie);

}