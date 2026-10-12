package sv.edu.ues.fmp.flora.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sv.edu.ues.fmp.flora.entity.VideoPreparacion;

/** Consultas de videos asociados a preparaciones de consumo. */
@Repository
public interface VideoPreparacionRepository extends JpaRepository<VideoPreparacion, Long> {


    List<VideoPreparacion> findByPreparacionIdPreparacionOrderByIdVideoPreparacionAsc(Long idPreparacion);


    List<VideoPreparacion> findByPreparacionIdPreparacionAndActivoTrueOrderByIdVideoPreparacionAsc(Long idPreparacion);
}

