package sv.edu.ues.fmp.flora.service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Year;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import sv.edu.ues.fmp.flora.config.UsuariosDePrueba;
import sv.edu.ues.fmp.flora.dto.response.UsuarioResponse;
import sv.edu.ues.fmp.flora.repository.UsuarioRepository;

/**
 * El rango del anio de {@code fuente} visto desde el endpoint: que un anio
 * fuera de rango sea un 400 de validacion y no el 409 de la base.
 *
 * <h2>Lo que estaba mal</h2>
 * {@code FuenteRequest} aceptaba desde el anio 1 y la columna tiene el CHECK
 * {@code ck_fuente_anio} --{@code anio >= 1000 AND anio <= 2100}--, asi que un
 * {@code POST} con anio 999 pasaba la validacion, llegaba al {@code INSERT} y
 * lo cortaba PostgreSQL. Ese {@code DataIntegrityViolationException} lo mapea
 * {@code GlobalExceptionHandler} a <strong>409</strong> con el mensaje de la
 * base, que no le dice al cliente que el problema es el anio. QA lo reporto
 * desde el otro lado: el formulario acepta anios que la base rechaza.
 *
 * <h2>Que fija esta clase y que fija la otra</h2>
 * {@code FuenteRequestAnioValidationTest} mide el rango sobre el objeto, con un
 * {@code Validator} y sin base. Esta mide lo que el bug tenia de propio y la
 * otra no puede ver: <strong>el codigo de estado</strong>. Por eso cada caso
 * afirma 400 y, ademas, que la respuesta trae la forma del error de validacion
 * --{@code erroresValidacion.anioValido}-- en vez del cuerpo de un conflicto.
 *
 * <h2>Cero residuo</h2>
 * {@code @SpringBootTest @Transactional}: las fuentes que crean los casos
 * validos viven en la transaccion de la prueba y el rollback se las lleva. Los
 * casos invalidos no insertan nada, que es justo lo que se esta comprobando. El
 * prefijo {@code Zzfuente} evita choques con {@code uk_fuente_titulo_lower}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FuenteAnioRangoTest {

    private static final String FUENTE_GESTIONAR = "FUENTE_GESTIONAR";

    private static final String MENSAJE = "El año debe estar entre 1000 y el año actual";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private EmailService emailService;

    private String token() {
        UsuarioResponse usuario = UsuarioResponse.builder()
                .id(idDeUsuarioActivo())
                .nombres("Prueba")
                .apellidos("FuenteAnio")
                // Dominio con punto por el CHECK ck_usuario_correo.
                .correo("prueba.fuenteanio@prueba.local")
                .nombreUsuario("prueba.fuenteanio")
                .activo(true)
                .build();

        return "Bearer " + jwtService
                .emitir(usuario, List.of("CONSULTOR"), List.of(FUENTE_GESTIONAR))
                .token();
    }

    // ---------- El limite inferior: el caso de QA ----------

    /**
     * El 999 es el que antes se iba al 409. Se afirma el 400 <em>y</em> que el
     * cuerpo sea el de un error de validacion: si manana alguien quitara la
     * validacion del DTO, la peticion volveria a morir en el CHECK y este caso
     * lo vería como un 409.
     */
    @Test
    void anio999_devuelve400DeValidacionYNoEl409DeLaBase() throws Exception {
        mockMvc.perform(post("/api/fuentes")
                        .header(HttpHeaders.AUTHORIZATION, token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Zzfuente anio 999", "999")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.error").value("Error de validación"))
                .andExpect(jsonPath("$.erroresValidacion.anioValido").value(MENSAJE));
    }

    @Test
    void anio1000_seCrea() throws Exception {
        mockMvc.perform(post("/api/fuentes")
                        .header(HttpHeaders.AUTHORIZATION, token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Zzfuente anio 1000", "1000")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.anio").value(1000));
    }

    // ---------- El limite superior: sigue en el anio actual ----------

    @Test
    void anioActual_seCrea() throws Exception {
        int actual = Year.now().getValue();

        mockMvc.perform(post("/api/fuentes")
                        .header(HttpHeaders.AUTHORIZATION, token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Zzfuente anio actual", String.valueOf(actual))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.anio").value(actual));
    }

    @Test
    void anioActualMasUno_devuelve400() throws Exception {
        int futuro = Year.now().getValue() + 1;

        mockMvc.perform(post("/api/fuentes")
                        .header(HttpHeaders.AUTHORIZATION, token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Zzfuente anio futuro", String.valueOf(futuro))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.erroresValidacion.anioValido").value(MENSAJE));
    }

    // ---------- El campo sigue siendo opcional ----------

    @Test
    void sinAnio_seCrea() throws Exception {
        mockMvc.perform(post("/api/fuentes")
                        .header(HttpHeaders.AUTHORIZATION, token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Zzfuente sin anio",
                                  "autor": "Pruebas automáticas",
                                  "tipoFuente": "OTRO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.anio").doesNotExist());
    }

    private static String cuerpo(String titulo, String anio) {
        return """
                {
                  "titulo": "%s",
                  "autor": "Pruebas automáticas",
                  "tipoFuente": "OTRO",
                  "anio": %s
                }
                """.formatted(titulo, anio);
    }

    /**
     * El token de estas pruebas firma a nombre de una cuenta real y activa.
     * <p>
     * Antes iba un id inventado (999): daba igual, porque nadie consultaba al
     * usuario. Con {@code VerificacionDeCuentaActiva} en la cadena ese token
     * recibe 401, y con razon --es el mismo caso que un token de una cuenta
     * borrada--. Ver {@link sv.edu.ues.fmp.flora.config.UsuariosDePrueba}.
     */
    @Autowired
    private UsuarioRepository usuarioRepositorioDePruebas;

    private Long idDeUsuarioActivo() {
        return UsuariosDePrueba.idActivo(usuarioRepositorioDePruebas);
    }
}
