package sv.edu.ues.fmp.flora.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

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
import sv.edu.ues.fmp.flora.config.SiembraDeFichas;
import sv.edu.ues.fmp.flora.config.SiembraDeFichas.Ficha;
import sv.edu.ues.fmp.flora.dto.response.UsuarioResponse;
import sv.edu.ues.fmp.flora.entity.Especie;
import sv.edu.ues.fmp.flora.entity.Usuario;
import sv.edu.ues.fmp.flora.entity.enums.EstadoPublicacion;
import sv.edu.ues.fmp.flora.repository.EspecieRepository;

/**
 * Editar una ficha PUBLICADA exige {@code ?confirmar=true}. Sin el parametro no
 * se cambia nada y la respuesta es un aviso que el front reconoce.
 *
 * <h2>Por que un aviso y no simplemente editar</h2>
 * Desde que editar invalida la revision previa
 * ({@code EdicionInvalidaLaRevisionTest}), un PUT sobre una ficha publicada la
 * saca del catalogo publico. Eso es correcto, pero es una consecuencia grande
 * para quien creia estar corrigiendo una errata: el backend la exige por
 * escrito antes de aplicarla.
 *
 * <h2>Como lo distingue el front, y por que importa</h2>
 * El aviso sale con 409, igual que un conflicto de estado, pero con el campo
 * {@code error} en {@code "Confirmación requerida"} en vez de
 * {@code "Transición de estado no válida"}. Es contrato: el cliente decide si
 * ofrecer "reenviar confirmando" leyendo ese campo, no el texto del mensaje, que
 * puede cambiar de redaccion sin avisar.
 * {@link #sinConfirmar_elErrorEsDistinguibleDelConflictoDeEstadoNormal()} es la
 * que fija esa diferencia comparando las dos respuestas en la misma prueba: si
 * alguien reutilizara {@code EstadoInvalidoException} para esto, las dos se
 * volverian indistinguibles y esa prueba es la que se cae.
 *
 * <h2>El alcance</h2>
 * Solo PUBLICADA. En BORRADOR, EN_REVISION y RECHAZADA la ficha no esta en el
 * catalogo publico y editar no pide nada; lo cubren los tres casos de
 * {@link #sinConfirmar_enLosEstadosQueNoSonPublicada_editaNormal()}. Incluido
 * EN_REVISION ya validada: ahi se pierde la validacion, pero la ficha no era
 * publica y revalidar es parte normal de la revision.
 *
 * <h2>Residuo</h2>
 * {@code @Transactional}: todo vive en la transaccion de la prueba y el
 * rollback se lo lleva.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConfirmacionAlEditarPublicadaTest {

    private static final String ESPECIE_EDITAR = "ESPECIE_EDITAR";
    private static final String ESPECIE_VALIDAR = "ESPECIE_VALIDAR";

    private static final String ERROR_CONFIRMACION = "Confirmación requerida";
    private static final String ERROR_ESTADO = "Transición de estado no válida";

    private static final String AVISO =
            "Esta ficha está publicada; al editarla saldrá del catálogo público y volverá "
                    + "a revisión. Reenvía con confirmar=true para continuar.";

    private static final AtomicLong SECUENCIA = new AtomicLong();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EspecieRepository especieRepository;

    @MockitoBean
    private EmailService emailService;

    private Usuario editor;

    @BeforeEach
    void usuario() {
        long n = SECUENCIA.incrementAndGet();

        editor = Usuario.builder()
                .nombres("Editor")
                .apellidos("Con Confirmación")
                .correo("editor.confirmacion." + n + "@prueba.local")
                .nombreUsuario("editor.confirmacion." + n)
                .claveHash("$2a$10$pruebaDeConfirmacionSinClaveUtilizable000000000000000")
                .activo(true)
                .build();

        entityManager.persist(editor);
        entityManager.flush();
    }

    // ---------- La prueba clave: sin confirmar no se toca nada ----------

    /**
     * Lo que no debe regresar: sin confirmar, la ficha queda exactamente como
     * estaba. No basta comprobar el 409; se comprueba que el contenido no se
     * guardo, que sigue PUBLICADA con sus dos responsables, y que sigue en el
     * catalogo publico.
     */
    @Test
    void putSinConfirmar_avisaYNoCambiaNada() throws Exception {
        Ficha ficha = sembrarPublicada();
        String descripcionOriginal = ficha.especie().getDescripcion();

        mockMvc.perform(put("/api/especies/" + ficha.id())
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPut(ficha, "Cambio que no debería aplicarse.")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.error").value(ERROR_CONFIRMACION))
                .andExpect(jsonPath("$.mensaje").value(AVISO))
                .andExpect(jsonPath("$.ruta").value("/api/especies/" + ficha.id()));

        Especie sinTocar = recargar(ficha.id());
        assertEquals(EstadoPublicacion.PUBLICADA, sinTocar.getEstadoPublicacion(),
                "la ficha salio del estado publicado sin que nadie lo confirmara");
        assertEquals(descripcionOriginal, sinTocar.getDescripcion(),
                "se guardo el contenido nuevo aunque la respuesta fue un 409");
        assertNotNull(sinTocar.getValidadaPor(), "se limpio el validador");
        assertNotNull(sinTocar.getFechaValidacion(), "se limpio la fecha de validacion");
        assertNotNull(sinTocar.getPublicadaPor(), "se limpio el publicador");
        assertNotNull(sinTocar.getFechaPublicacion(), "se limpio la fecha de publicacion");
        assertTrue(estaEnPublicadas(ficha.id()), "la ficha salio del catálogo público");
    }

    @Test
    void patchTaxonomiaSinConfirmar_avisaYNoCambiaNada() throws Exception {
        Ficha ficha = sembrarPublicada();
        String familiaOriginal = ficha.especie().getTaxonomia().getFamilia();

        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/taxonomia")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoTaxonomia(ficha, "Noaplicaceae")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.error").value(ERROR_CONFIRMACION))
                .andExpect(jsonPath("$.mensaje").value(AVISO));

        Especie sinTocar = recargar(ficha.id());
        assertEquals(EstadoPublicacion.PUBLICADA, sinTocar.getEstadoPublicacion());
        assertEquals(familiaOriginal, sinTocar.getTaxonomia().getFamilia(),
                "se guardo la familia nueva aunque la respuesta fue un 409");
        assertNotNull(sinTocar.getPublicadaPor(), "se limpio el publicador");
        assertTrue(estaEnPublicadas(ficha.id()), "la ficha salio del catálogo público");
    }

    /**
     * La diferencia que el front necesita, comprobada contra la otra respuesta
     * de 409 del mismo flujo en una sola prueba: mismo codigo, {@code error}
     * distinto.
     */
    @Test
    void sinConfirmar_elErrorEsDistinguibleDelConflictoDeEstadoNormal() throws Exception {
        // a. El aviso de confirmacion, sobre una ficha publicada.
        Ficha publicada = sembrarPublicada();
        mockMvc.perform(put("/api/especies/" + publicada.id())
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPut(publicada, "Da igual.")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(ERROR_CONFIRMACION));

        // b. Un conflicto de estado de verdad: validar una ficha en BORRADOR.
        Ficha borrador = new SiembraDeFichas(entityManager).sembrar(EstadoPublicacion.BORRADOR);
        mockMvc.perform(patch("/api/especies/" + borrador.id() + "/validar")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_VALIDAR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(ERROR_ESTADO));

        // Los dos son 409; lo que los separa es el campo 'error', y eso es lo
        // que el front lee. Si alguien hiciera que la confirmacion saliera por
        // EstadoInvalidoException, aqui se veria.
    }

    // ---------- Con confirmar, procede ----------

    @Test
    void putConConfirmar_editaYLaDevuelveARevision() throws Exception {
        Ficha ficha = sembrarPublicada();

        mockMvc.perform(put("/api/especies/" + ficha.id())
                        .param("confirmar", "true")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPut(ficha, "Cambio confirmado.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPublicacion").value("EN_REVISION"))
                .andExpect(jsonPath("$.validadaPor").doesNotExist())
                .andExpect(jsonPath("$.publicadaPor").doesNotExist());

        Especie guardada = recargar(ficha.id());
        assertEquals(EstadoPublicacion.EN_REVISION, guardada.getEstadoPublicacion());
        assertEquals("Cambio confirmado.", guardada.getDescripcion());
        assertTrue(!estaEnPublicadas(ficha.id()), "sigue en el catálogo público");
    }

    @Test
    void patchTaxonomiaConConfirmar_editaYLaDevuelveARevision() throws Exception {
        Ficha ficha = sembrarPublicada();

        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/taxonomia")
                        .param("confirmar", "true")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoTaxonomia(ficha, "Confirmadaceae")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPublicacion").value("EN_REVISION"))
                .andExpect(jsonPath("$.taxonomia.familia").value("Confirmadaceae"));

        assertEquals(EstadoPublicacion.EN_REVISION, recargar(ficha.id()).getEstadoPublicacion());
    }

    /**
     * {@code confirmar=false} explicito es lo mismo que no mandarlo: el defecto
     * es false y no hay que escribirlo para que proteja.
     */
    @Test
    void confirmarFalseExplicito_avisaIgual() throws Exception {
        Ficha ficha = sembrarPublicada();

        mockMvc.perform(put("/api/especies/" + ficha.id())
                        .param("confirmar", "false")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPut(ficha, "Da igual.")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(ERROR_CONFIRMACION));

        assertEquals(EstadoPublicacion.PUBLICADA, recargar(ficha.id()).getEstadoPublicacion());
    }

    // ---------- Los estados que no piden confirmacion ----------

    @Test
    void sinConfirmar_enLosEstadosQueNoSonPublicada_editaNormal() throws Exception {
        for (EstadoPublicacion estado : List.of(EstadoPublicacion.BORRADOR,
                EstadoPublicacion.EN_REVISION, EstadoPublicacion.RECHAZADA)) {

            Ficha ficha = new SiembraDeFichas(entityManager).sembrar(estado);

            mockMvc.perform(put("/api/especies/" + ficha.id())
                            .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoPut(ficha, "Editado sin confirmar, en " + estado)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estadoPublicacion").value(estado.name()));

            assertEquals(estado, recargar(ficha.id()).getEstadoPublicacion(),
                    "editar en " + estado + " movio el estado");
            assertEquals("Editado sin confirmar, en " + estado,
                    recargar(ficha.id()).getDescripcion(),
                    "no se guardo el cambio en " + estado);
        }
    }

    /**
     * El caso que podria parecer que necesita confirmacion y no la necesita:
     * EN_REVISION ya validada pierde la validacion al editar, pero la ficha no
     * era publica.
     */
    @Test
    void sinConfirmar_enRevisionYaValidada_editaYPierdeLaValidacion() throws Exception {
        Ficha ficha = new SiembraDeFichas(entityManager).sembrar(EstadoPublicacion.EN_REVISION);

        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/validar")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_VALIDAR)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/especies/" + ficha.id())
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPut(ficha, "Editado durante la revisión.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validadaPor").doesNotExist());

        assertEquals(EstadoPublicacion.EN_REVISION, recargar(ficha.id()).getEstadoPublicacion());
    }

    // ---------- El orden: la seguridad va antes ----------

    /**
     * La confirmacion se comprueba dentro del servicio, asi que la autorizacion
     * la precede: quien no puede editar recibe 401 o 403, nunca el aviso de
     * confirmacion, que le diria que basta reenviar con un parametro.
     */
    @Test
    void laAutorizacionVaAntesDelAviso() throws Exception {
        Ficha ficha = sembrarPublicada();

        // Anonimo: 401, no 409.
        mockMvc.perform(put("/api/especies/" + ficha.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPut(ficha, "Anónimo.")))
                .andExpect(status().isUnauthorized());

        // Con token pero sin ESPECIE_EDITAR: 403, no 409.
        mockMvc.perform(put("/api/especies/" + ficha.id())
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_VALIDAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPut(ficha, "Sin permiso de editar.")))
                .andExpect(status().isForbidden());

        // Y lo mismo con confirmar=true: el parametro no abre ninguna puerta.
        mockMvc.perform(put("/api/especies/" + ficha.id())
                        .param("confirmar", "true")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_VALIDAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPut(ficha, "Confirmando sin permiso.")))
                .andExpect(status().isForbidden());

        Especie sinTocar = recargar(ficha.id());
        assertEquals(EstadoPublicacion.PUBLICADA, sinTocar.getEstadoPublicacion());
        assertNotNull(sinTocar.getPublicadaPor(), "un 401 o un 403 limpio el publicador");
    }

    // ---------- Apoyo ----------

    private Ficha sembrarPublicada() {
        return new SiembraDeFichas(entityManager).sembrar(EstadoPublicacion.PUBLICADA);
    }

    private String token(String... permisos) {
        UsuarioResponse usuario = UsuarioResponse.builder()
                .id(editor.getIdUsuario())
                .nombres(editor.getNombres())
                .apellidos(editor.getApellidos())
                .correo(editor.getCorreo())
                .nombreUsuario(editor.getNombreUsuario())
                .activo(true)
                .build();

        return "Bearer "
                + jwtService.emitir(usuario, List.of("CONSULTOR"), List.of(permisos)).token();
    }

    private boolean estaEnPublicadas(Long id) throws Exception {
        String cuerpo = mockMvc.perform(get("/api/especies/publicadas"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return cuerpo.contains("\"idEspecie\":" + id + ",");
    }

    private static String cuerpoPut(Ficha ficha, String descripcion) {
        return """
                {
                  "nombreCientifico": "%s",
                  "descripcion": "%s",
                  "taxonomia": {
                    "genero": "%s",
                    "especieTaxonomica": "%s",
                    "familia": "%s"
                  }
                }
                """.formatted(
                ficha.especie().getNombreCientifico(),
                descripcion,
                ficha.especie().getTaxonomia().getGenero(),
                ficha.especie().getTaxonomia().getEspecieTaxonomica(),
                ficha.especie().getTaxonomia().getFamilia());
    }

    private static String cuerpoTaxonomia(Ficha ficha, String familia) {
        return """
                {
                  "genero": "%s",
                  "especieTaxonomica": "%s",
                  "familia": "%s"
                }
                """.formatted(
                ficha.especie().getTaxonomia().getGenero(),
                ficha.especie().getTaxonomia().getEspecieTaxonomica(),
                familia);
    }

    private Especie recargar(Long id) {
        entityManager.flush();
        entityManager.clear();
        return especieRepository.findById(id).orElseThrow();
    }
}
