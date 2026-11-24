package sv.edu.ues.fmp.flora.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sv.edu.ues.fmp.flora.entity.ImagenEspecie;

/** Consultas de imágenes por especie y de su única principal activa. */
@Repository
public interface ImagenEspecieRepository extends JpaRepository<ImagenEspecie, Long> {
    /** Incluye el historial de imágenes desactivadas. */
    List<ImagenEspecie> findByEspecieIdEspecieOrderByIdImagenAsc(Long idEspecie);

    /** Excluye las bajas lógicas y mantiene un orden estable. */
    List<ImagenEspecie> findByEspecieIdEspecieAndActivaTrueOrderByIdImagenAsc(Long idEspecie);

    /** Como máximo una fila por el índice parcial uk_imagen_principal_especie. */
    Optional<ImagenEspecie> findByEspecieIdEspecieAndEsPrincipalTrueAndActivaTrue(Long idEspecie);
}
