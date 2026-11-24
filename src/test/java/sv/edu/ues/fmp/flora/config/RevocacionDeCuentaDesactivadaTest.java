package sv.edu.ues.fmp.flora.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import sv.edu.ues.fmp.flora.config.SiembraDeFichas.Ficha;
import sv.edu.ues.fmp.flora.dto.response.UsuarioResponse;
import sv.edu.ues.fmp.flora.entity.Usuario;
import sv.edu.ues.fmp.flora.entity.enums.EstadoPublicacion;
import sv.edu.ues.fmp.flora.repository.UsuarioRepository;
import sv.edu.ues.fmp.flora.service.EmailService;
import sv.edu.ues.fmp.flora.service.JwtService;

/**
 * Revocacion inmediata: la cuenta desactivada deja de operar en la siguiente
 * peticion, no al caducar su token.
 *
 * <h2>Lo que estaba mal (SEG-110, SEG-111, SEG-PB-07)</h2>
 * El token es sin estado y dura una hora. El {@code JwtDecoder} le comprueba
 * firma y vigencia, y nadie volvia a preguntar por la cuenta, asi que
 * desactivar a alguien solo le impedia <strong>volver</strong> a iniciar
 * sesion: el token que ya tenia seguia abriendo puertas hasta caducar. QA lo
 * encontro por tres: listar usuarios (SEG-110, un GET), cambiar su propia clave
 * (SEG-111) y crear parte-beneficio (SEG-PB-07). Justo el margen que no debe
 * existir cuando alguien deja el proyecto o se sospecha de su credencial.
 *
 * <h2>Como se prueba</h2>
 * Cada caso emite el token de una cuenta real y activa --que es lo que el
 * filtro exige, ver {@link UsuariosDePrueba}-- y <strong>despues</strong>
 * desactiva esa cuenta, que es la secuencia que vivio QA: el token se firmo
 * cuando la cuenta valia. El token no se vuelve a pedir, porque el punto es
 * que el <em>mismo</em> token deje de servir.
 *
 * <h2>Lo que no debe cambiar</h2>
 * Tres casos cuidan de que el filtro no se pase de celo: la cuenta activa sigue
 * operando, el anonimo sigue entrando al catalogo publico --el filtro no mira
 * la peticion sin principal-- y el permiso {@code ESPECIE_CONSULTAR} sigue
 * mostrando lo no publicado, o sea que el filtro no estropeo el filtro de
 * visibilidad que se monto antes.
 *
 * <h2>Cero residuo</h2>
 * {@code @SpringBootTest @Transactional}: la desactivacion de la cuenta, la
 * ficha sembrada y lo que creen los casos viven en la transaccion de la prueba
 * y el rollback se lo lleva. Ninguna cuenta real queda desactivada al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RevocacionDeCuentaDesactivadaTest {

    private static final String USUARIO_GESTIONAR = "USUARIO_GESTIONAR";

    private static final String PARTE_GESTIONAR = "PARTE_GESTIONAR";

    private static final String ESPECIE_CONSULTAR = "ESPECIE_CONSULTAR";

    /** Un id que no corresponde a ninguna cuenta: el caso del usuario borrado. */
    private static final long ID_SIN_CUENTA = 999999L;

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EntityManager entityManager;

    @MockitoBean private EmailService emailService;

    private Ficha publicada;
    private Ficha borrador;

    @BeforeEach
    void sembrar() {
        SiembraDeFichas siembra = new SiembraDeFichas(entityManager);

        publicada = siembra.sembrar(EstadoPublicacion.PUBLICADA);
        borrador = siembra.sembrar(EstadoPublicacion.BORRADOR);
    }

    // ---------- 1. SEG-110: el GET que seguia respondiendo ----------

    @Test
    void usuarioDesactivado_listarUsuarios_devuelve401() throws Exception {
        Long id = cuentaActiva();
        String token = tokenDe(id, USUARIO_GESTIONAR);

        // Con la cuenta todavia activa el token sirve: asi se sabe que el 401
        // de abajo lo produjo la desactivacion y no un token mal armado.
        mockMvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());

        desactivar(id);

        mockMvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.error").value("No autorizado"));
    }

    // ---------- 2. SEG-111: cambiar su propia clave ----------

    /**
     * {@code cambiarClave} no comprueba el estado de la cuenta --y no hizo
     * falta tocarlo--: el filtro corre antes del controlador, asi que la
     * peticion ni llega.
     */
    @Test
    void usuarioDesactivado_cambiarSuClave_devuelve401() throws Exception {
        Long id = cuentaActiva();
        String token = tokenDe(id, USUARIO_GESTIONAR);

        desactivar(id);

        mockMvc.perform(put("/api/usuarios/" + id + "/clave")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "claveActual": "irrelevante",
                                  "claveNueva": "TambienIrrelevante1",
                                  "confirmacionClaveNueva": "TambienIrrelevante1"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    // ---------- 3. SEG-PB-07: la escritura de catalogo ----------

    @Test
    void usuarioDesactivado_crearParteBeneficio_devuelve401() throws Exception {
        Long id = cuentaActiva();
        String token = tokenDe(id, PARTE_GESTIONAR);

        desactivar(id);

        mockMvc.perform(post("/api/parte-beneficios")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idEspecieParte": %d,
                                  "idBeneficio": %d,
                                  "idFuente": 1,
                                  "observacion": "No deberia crearse."
                                }
                                """.formatted(
                                        publicada.parte().getIdEspecieParte(),
                                        publicada.parteBeneficio().getId().getIdBeneficio())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    // ---------- 4. La cuenta activa sigue trabajando ----------

    @Test
    void usuarioActivo_susOperacionesSiguenFuncionando() throws Exception {
        String token = tokenDe(cuentaActiva(), USUARIO_GESTIONAR, ESPECIE_CONSULTAR);

        mockMvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/especies").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/parte-beneficios").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    // ---------- 5. El anonimo no lo toca el filtro ----------

    @Test
    void anonimo_rutaPublicaDelCatalogo_sigueEn200() throws Exception {
        mockMvc.perform(get("/api/beneficios")).andExpect(status().isOk());
        mockMvc.perform(get("/api/partes-planta")).andExpect(status().isOk());
        mockMvc.perform(get("/api/especies/publicadas")).andExpect(status().isOk());

        // Y el listado por parte, que entra por el id del padre.
        mockMvc.perform(get("/api/parte-beneficios/parte/"
                        + publicada.parte().getIdEspecieParte()))
                .andExpect(status().isOk());
    }

    // ---------- 6. El filtro de visibilidad no se rompio ----------

    @Test
    void usuarioActivoConEspecieConsultar_sigueViendoLoNoPublicado() throws Exception {
        String token = tokenDe(cuentaActiva(), ESPECIE_CONSULTAR);

        mockMvc.perform(get("/api/parte-beneficios/parte/"
                        + borrador.parte().getIdEspecieParte())
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Y sin token, la misma parte sigue siendo invisible.
        mockMvc.perform(get("/api/parte-beneficios/parte/"
                        + borrador.parte().getIdEspecieParte()))
                .andExpect(status().isNotFound());
    }

    // ---------- 7. La cuenta que ya no existe ----------

    @Test
    void tokenDeUnaCuentaQueNoExiste_devuelve401() throws Exception {
        String token = tokenDe(ID_SIN_CUENTA, USUARIO_GESTIONAR);

        mockMvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    /**
     * El 401 de la cuenta desactivada y el de la cuenta inexistente tienen que
     * ser el mismo cuerpo, y el mismo que el de un token roto: distinguirlos le
     * diria a quien prueba tokens cual de sus intentos corresponde a una cuenta
     * que existe.
     */
    @Test
    void losTresMotivosDe401_respondenLoMismo() throws Exception {
        Long id = cuentaActiva();
        String token = tokenDe(id, USUARIO_GESTIONAR);
        desactivar(id);

        String desactivada = cuerpo401(token);
        String inexistente = cuerpo401(tokenDe(ID_SIN_CUENTA, USUARIO_GESTIONAR));
        String roto = cuerpo401("Bearer esto-no-es-un-jwt");

        assertEquals(normalizar(inexistente), normalizar(desactivada),
                "la cuenta desactivada y la inexistente deben ser indistinguibles");
        assertEquals(normalizar(roto), normalizar(desactivada),
                "y las dos, indistinguibles de un token que no verifica");
    }

    // ---------- Armado ----------

    /** El id de una cuenta real y activa, la que firma los tokens. */
    private Long cuentaActiva() {
        return UsuariosDePrueba.idActivo(usuarioRepository);
    }

    /**
     * Desactiva la cuenta y adelanta el UPDATE: MockMvc corre en la misma
     * transaccion, pero el filtro consulta por SQL y no veria lo que siguiera
     * en la cola de Hibernate.
     */
    private void desactivar(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow();
        usuario.setActivo(false);
        usuarioRepository.saveAndFlush(usuario);
    }

    private String tokenDe(Long idUsuario, String... permisos) {
        UsuarioResponse usuario = UsuarioResponse.builder()
                .id(idUsuario)
                .nombres("Prueba")
                .apellidos("Revocacion")
                // Dominio con punto por el CHECK ck_usuario_correo.
                .correo("prueba.revocacion@prueba.local")
                .nombreUsuario("prueba.revocacion")
                .activo(true)
                .build();

        return "Bearer " + jwtService
                .emitir(usuario, List.of("CONSULTOR"), List.of(permisos))
                .token();
    }

    private String cuerpo401(String token) throws Exception {
        return mockMvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    /** Borra lo que por fuerza difiere: el instante. */
    private static String normalizar(String cuerpo) {
        return cuerpo.replaceAll("\"timestamp\":\"[^\"]*\"", "\"timestamp\":\"{instante}\"");
    }
}
