package sv.edu.ues.fmp.flora.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sv.edu.ues.fmp.flora.entity.TokenRecuperacionClave;

@Repository
public interface TokenRecuperacionClaveRepository extends JpaRepository<TokenRecuperacionClave, Long> {

    Optional<TokenRecuperacionClave> findByTokenHash(String tokenHash);

    List<TokenRecuperacionClave> findByUsuario_IdUsuarioAndUsadoFalse(Long idUsuario);
}
