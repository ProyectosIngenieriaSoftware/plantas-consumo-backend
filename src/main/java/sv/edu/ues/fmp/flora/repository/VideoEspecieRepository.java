package sv.edu.ues.fmp.flora.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sv.edu.ues.fmp.flora.entity.VideoEspecie;

@Repository
public interface VideoEspecieRepository extends JpaRepository<VideoEspecie, Long> {

    List<VideoEspecie> findByEspecieIdEspecieOrderByIdVideoAsc(Long idEspecie);

    List<VideoEspecie> findByEspecieIdEspecieAndActivoTrueOrderByIdVideoAsc(Long idEspecie);
}
