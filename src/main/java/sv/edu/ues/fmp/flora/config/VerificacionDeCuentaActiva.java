package sv.edu.ues.fmp.flora.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.entity.Usuario;
import sv.edu.ues.fmp.flora.exception.AutenticacionEntryPoint;
import sv.edu.ues.fmp.flora.exception.CredencialesInvalidasException;
import sv.edu.ues.fmp.flora.repository.UsuarioRepository;
import sv.edu.ues.fmp.flora.service.UsuarioAutenticado;

/**
 * Revocacion inmediata: una cuenta desactivada deja de operar en la siguiente
 * peticion, no al caducar su token.
 *
 * <h2>El agujero que cierra (SEG-110, SEG-111, SEG-PB-07)</h2>
 * El token es sin estado y vale una hora: lleva dentro el id, los roles y los
 * permisos de quien inicio sesion, y el {@code JwtDecoder} solo le comprueba
 * firma y vigencia. Desactivar a alguien impedia que <strong>volviera</strong> a
 * iniciar sesion --{@code UsuarioServiceImpl.iniciarSesion} filtra por
 * {@code activo}-- pero no invalidaba el token que ya tenia en la mano. QA lo
 * encontro por tres puertas distintas: un usuario desactivado seguia listando
 * usuarios (SEG-110, un GET), cambiando su propia clave (SEG-111) y creando
 * parte-beneficio (SEG-PB-07). Hasta sesenta minutos de margen, justo en el
 * momento en que alguien deja el proyecto o se sospecha que su credencial se
 * filtro.
 *
 * <h2>Que comprueba, y que no</h2>
 * Solo <strong>existencia y estado activo</strong> de la cuenta. Los roles y
 * los permisos <strong>no</strong> se vuelven a consultar: que quitarle un
 * permiso a un rol tarde hasta el proximo login (SEG-106 y SEG-108) es el
 * precio aceptado de no ir a la base por la matriz de autorizacion en cada
 * peticion, y queda fuera del alcance de este filtro a proposito. Lo que cambia
 * aqui es la <em>identidad</em>, no la autorizacion: desactivar una cuenta es
 * la operacion con la que se corta el acceso de alguien, y esa si tiene que
 * surtir efecto ya.
 *
 * <h2>Por que no rompe nada publico</h2>
 * El filtro solo mira la peticion que ya trae un principal de tipo
 * {@link JwtAuthenticationToken}. Una peticion anonima --el catalogo publico,
 * el login, los tres POST abiertos de usuarios-- no tiene ese principal y el
 * filtro la deja pasar sin tocarla y sin consultar la base. Tampoco se mete con
 * {@code @PreAuthorize}: corre antes, asi que un usuario desactivado recibe 401
 * y nunca llega a la comprobacion del permiso.
 * <p>
 * Un caso que conviene conocer: un usuario desactivado que pida una ruta
 * <em>publica</em> del catalogo <strong>con</strong> su token recibe 401, no
 * 200. Es coherente con lo que ya hacia la aplicacion --un token invalido en
 * una ruta publica tambien da 401, porque el resolvedor solo ignora la cabecera
 * en {@code PUBLICAS}-- y la salida es la misma de siempre: el cliente no debe
 * mandar token en las consultas publicas, y ante un 401 debe descartarlo.
 *
 * <h2>Por que no es un @Component</h2>
 * Lo declara {@code SecurityConfig} como {@code @Bean}. Si se registrara
 * solo, {@code @WebMvcTest} lo instanciaria: ese corte del contexto incluye
 * a proposito los beans de tipo {@code Filter}, pero no los repositorios JPA,
 * asi que el filtro no encontraria el {@code UsuarioRepository} y las pruebas
 * de controlador cortadas no arrancarian. Declarandolo donde se usa, el bean
 * existe solo en el contexto que tambien trae la cadena de seguridad.
 *
 * <h2>El costo</h2>
 * Un {@code findById} por peticion autenticada, por llave primaria. A este
 * volumen no se nota, y es el precio de que el corte sea inmediato. Las
 * peticiones anonimas no pagan nada.
 *
 * <h2>Por que el 401 sale por el entry point y no por una excepcion</h2>
 * Un filtro corre dentro de la cadena de Spring Security, antes del
 * {@code DispatcherServlet}, asi que el {@code GlobalExceptionHandler} no lo ve:
 * una excepcion lanzada aqui saldria como un 500 con el formato de error del
 * contenedor. Por eso se llama directamente a
 * {@link AutenticacionEntryPoint}, el mismo que ya responde el token ausente,
 * invalido o expirado, y el cuerpo del 401 es byte por byte el que el cliente
 * ya sabe leer. Ese mensaje es generico a proposito: no distingue "la cuenta no
 * existe" de "la cuenta esta desactivada" de "el token no verifica", porque la
 * diferencia le serviria a quien esta probando tokens a ciegas.
 */
@RequiredArgsConstructor
public class VerificacionDeCuentaActiva extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(VerificacionDeCuentaActiva.class);

    private final UsuarioRepository usuarioRepository;

    /**
     * El id de quien llama, resuelto en el mismo sitio que lo resuelve el resto
     * del proyecto. Se reusa en vez de leer el {@code sub} aqui para no abrir
     * un segundo criterio de identidad.
     */
    private final UsuarioAutenticado usuarioAutenticado;

    private final AutenticacionEntryPoint autenticacionEntryPoint;

    @Override
    protected void doFilterInternal(
            HttpServletRequest peticion,
            HttpServletResponse respuesta,
            FilterChain cadena) throws ServletException, IOException {

        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();

        // Regla 1: sin principal JWT no hay nada que revalidar. Cubre de una vez
        // el contexto vacio, el AnonymousAuthenticationToken de las rutas
        // publicas y cualquier otro tipo de autenticacion que se agregue luego.
        if (!(autenticacion instanceof JwtAuthenticationToken)) {
            cadena.doFilter(peticion, respuesta);
            return;
        }

        Long idUsuario;
        try {
            idUsuario = usuarioAutenticado.id();
        } catch (CredencialesInvalidasException subInutilizable) {
            // El token verifica firma y vigencia, pero su sub no es un id: no
            // hay cuenta que comprobar, asi que no se deja pasar.
            rechazar(peticion, respuesta, "el sub del token no es un id de usuario");
            return;
        }

        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);

        // Regla 3: existencia y estado activo, nada mas. Ni roles ni permisos.
        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())) {
            rechazar(peticion, respuesta, "la cuenta " + idUsuario
                    + (usuario == null ? " ya no existe" : " esta desactivada"));
            return;
        }

        cadena.doFilter(peticion, respuesta);
    }

    /**
     * Corta la peticion con el 401 de siempre.
     * <p>
     * Se limpia el contexto antes de responder: la peticion no sigue, pero
     * dejar un principal autenticado puesto para algo que se acaba de rechazar
     * invita a que cualquier cosa que corra despues lo crea bueno.
     */
    private void rechazar(
            HttpServletRequest peticion,
            HttpServletResponse respuesta,
            String motivo) throws IOException {

        // El motivo queda en el log y no en la respuesta, igual que hace
        // AutenticacionEntryPoint con la causa de un token invalido.
        log.debug("Token rechazado por revocacion: {} ({} {})",
                motivo, peticion.getMethod(), peticion.getRequestURI());

        SecurityContextHolder.clearContext();

        autenticacionEntryPoint.commence(peticion, respuesta,
                new DisabledException("La cuenta del token ya no puede operar"));
    }
}
