package sv.edu.ues.fmp.flora.repository;

import sv.edu.ues.fmp.flora.entity.Distrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistritoRepository extends JpaRepository<Distrito, Long> {
   // List<Distrito> findByDepartamento(String departamento);
}