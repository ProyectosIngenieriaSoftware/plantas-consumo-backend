package sv.edu.ues.fmp.flora.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sv.edu.ues.fmp.flora.entity.Municipio;

import java.util.List;
import java.util.Optional;

@Repository
public interface MunicipioRepository extends JpaRepository<Municipio, Long> {

    //WHERE m.id_departamento = ? AND lower(m.nombre) = lower(?)
    boolean existsByDepartamentoIdDepartamentoAndNombreIgnoreCase(Long idDepartamento, String nombre);

    // Nos permite ver si realmente hay algo en la lista y no tener un NullPointerException si no trae nada
    Optional<Municipio> findByDepartamentoIdDepartamentoAndNombreIgnoreCase(Long idDepartamento, String nombre);

    List<Municipio> findByActivoTrue();

    List<Municipio> findByDepartamentoIdDepartamento(Long idDepartamento);
}
