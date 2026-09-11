package sv.edu.ues.fmp.flora.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sv.edu.ues.fmp.flora.entity.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByNombreUsuarioIgnoreCase(String nombreUsuario);

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    Optional<Usuario> findByNombreUsuarioIgnoreCase(String nombreUsuario);

    List<Usuario> findByActivoTrue();

    Optional<Usuario> findByCorreoIgnoreCaseOrNombreUsuarioIgnoreCase(String correo, String nombreUsuario);
}
