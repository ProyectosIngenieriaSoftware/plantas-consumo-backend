package sv.edu.ues.fmp.flora.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.dto.request.RestablecerClaveRequest;
import sv.edu.ues.fmp.flora.dto.request.SolicitudRecuperacionClaveRequest;
import sv.edu.ues.fmp.flora.entity.TokenRecuperacionClave;
import sv.edu.ues.fmp.flora.entity.Usuario;
import sv.edu.ues.fmp.flora.exception.TokenInvalidoException;
import sv.edu.ues.fmp.flora.repository.TokenRecuperacionClaveRepository;
import sv.edu.ues.fmp.flora.repository.UsuarioRepository;
import sv.edu.ues.fmp.flora.service.EmailService;
import sv.edu.ues.fmp.flora.service.RecuperacionClaveService;
import sv.edu.ues.fmp.flora.util.Tokens;

/**
 * Flujo de "olvidé mi contraseña".
 * <p>
 * {@link #solicitarRecuperacion} nunca revela si el correo existe o no: el
 * controlador responde siempre el mismo 202, exista o no la cuenta. Es el
 * mismo criterio que ya se sigue en {@code UsuarioServiceImpl#iniciarSesion},
 * para que este endpoint no sirva para averiguar qué correos están
 * registrados.
 */
@Service
@RequiredArgsConstructor
public class RecuperacionClaveServiceImpl implements RecuperacionClaveService {

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacionClaveRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.recuperacion-clave.url-base}")
    private String urlBase;

    @Value("${app.recuperacion-clave.expiracion-minutos}")
    private int expiracionMinutos;

    @Override
    @Transactional
    public void solicitarRecuperacion(SolicitudRecuperacionClaveRequest request) {
        usuarioRepository.findByCorreoIgnoreCase(request.correo())
                .filter(Usuario::getActivo)
                .ifPresent(this::generarYEnviarToken);
    }

    private void generarYEnviarToken(Usuario usuario) {
        // Invalida cualquier token anterior sin usar: solo el ultimo enlace
        // enviado debe quedar vigente.
        List<TokenRecuperacionClave> vigentes =
                tokenRepository.findByUsuario_IdUsuarioAndUsadoFalse(usuario.getIdUsuario());
        vigentes.forEach(token -> token.setUsado(true));

        String tokenCrudo = Tokens.generar();

        TokenRecuperacionClave nuevo = TokenRecuperacionClave.builder()
                .usuario(usuario)
                .tokenHash(Tokens.hash(tokenCrudo))
                .fechaExpiracion(LocalDateTime.now().plusMinutes(expiracionMinutos))
                .build();
        tokenRepository.save(nuevo);

        String enlace = urlBase + "?token=" + tokenCrudo;
        emailService.enviarCorreoRecuperacion(usuario.getCorreo(), usuario.getNombres(), enlace);
    }

    @Override
    @Transactional
    public void restablecerClave(RestablecerClaveRequest request) {
        String hash = Tokens.hash(request.token());

        TokenRecuperacionClave token = tokenRepository.findByTokenHash(hash)
                .filter(t -> !t.getUsado())
                .filter(t -> t.getFechaExpiracion().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new TokenInvalidoException(
                        "El enlace de recuperación no es válido o ya expiró"));

        Usuario usuario = token.getUsuario();
        usuario.setClaveHash(passwordEncoder.encode(request.claveNueva()));
        usuarioRepository.saveAndFlush(usuario);

        token.setUsado(true);
    }
}
