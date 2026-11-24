package sv.edu.ues.fmp.flora.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sv.edu.ues.fmp.flora.entity.ImagenPreparacion;

/** Consultas de imagenes asociadas a preparaciones de consumo. */
@Repository
public interface ImagenPreparacionRepository extends JpaRepository<ImagenPreparacion, Long> {


    List<ImagenPreparacion> findByPreparacionIdPreparacionOrderByIdImagenPreparacionAsc(Long idPreparacion);

    List<ImagenPreparacion> findByPreparacionIdPreparacionAndActivaTrueOrderByIdImagenPreparacionAsc(Long idPreparacion);


    Optional<ImagenPreparacion> findByPreparacionIdPreparacionAndEsPrincipalTrueAndActivaTrue(Long idPreparacion);


    long countByPreparacionIdPreparacionAndActivaTrue(Long idPreparacion);
}

