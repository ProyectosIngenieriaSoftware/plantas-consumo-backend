package sv.edu.ues.fmp.flora.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sv.edu.ues.fmp.flora.entity.EspecieFuente;
import sv.edu.ues.fmp.flora.entity.EspecieFuenteId;

@Repository
public interface EspecieFuenteRepository extends JpaRepository<EspecieFuente, EspecieFuenteId> {

    List<EspecieFuente> findByEspecie_IdEspecie(Long idEspecie);
}