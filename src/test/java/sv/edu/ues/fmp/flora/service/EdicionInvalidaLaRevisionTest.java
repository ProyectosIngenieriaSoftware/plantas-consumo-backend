package sv.edu.ues.fmp.flora.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * Editar el contenido de una ficha invalida su revision previa: decision del
 * equipo con Agronomia. Un cambio sobre contenido publicado o ya validado
 * obliga a revalidar y volver a publicar antes de que la ficha sea publica otra
 * vez.
 *
 * <h2>El agujero que cierra</h2>
 * Ni {@code actualizar} (PUT) ni {@code actualizarTaxonomia}
 * (PATCH /taxonomia) tenian guarda de estado ni tocaban los campos de control
 * --el comentario del servicio lo decia explicito: "eso solo cambia por las
 * transiciones"--, asi que una ficha PUBLICADA se reescribia entera y seguia
 * publicada, con un contenido que ningun revisor habia visto. Bastaba
 * {@code ESPECIE_EDITAR}. Lo que se publicaba y lo que se habia aprobado eran
 * dos cosas distintas y nada en el flujo lo delataba.
 *
 * <h2>Las dos pruebas que no pasan por casualidad</h2>
 * {@link #editarUnaPublicada_laSacaDelCatalogoPublico()} comprueba el efecto
 * donde de verdad importa: la ficha desaparece de
 * {@code GET /api/especies/publicadas}, que es el catalogo que ve cualquiera.
 * Y {@link #cicloCompleto_publicadaEditarRevalidarRepublicar()} recorre la
 * vuelta entera, porque una regla que invalide la revision pero deje la ficha
 * sin forma de volver a publicarse seria igual de inservible.
 *
 * <h2>El alcance</h2>
 * Solo las dos ediciones del contenido propio de la especie. Las ediciones de
 * sub-recursos (partes, aportes, epocas, beneficios, nombres comunes,
 * multimedia) quedan deliberadamente fuera, y
 * {@link #editarUnSubRecurso_noInvalidaLaRevision()} lo fija como el limite
 * que es: si algun dia se decide incluirlas, esa prueba es la que avisa de que
 * el alcance cambio, en vez de que el cambio pase inadvertido.
 *
 * <h2>Residuo</h2>
 * {@code @Transactional}: todo vive en la transaccion de la prueba y el
 * rollback se lo lleva.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EdicionInvalidaLaRevisionTest {

    private static final String ESPECIE_CONSULTAR = "ESPECIE_CONSULTAR";
    private static final String ESPECIE_EDITAR = "ESPECIE_EDITAR";
    private static final String ESPECIE_VALIDAR = "ESPECIE_VALIDAR";
    private static final String ESPECIE_PUBLICAR = "ESPECIE_PUBLICAR";

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
                .apellidos("De Contenido")
                .correo("editor.contenido." + n + "@prueba.local")
                .nombreUsuario("editor.contenido." + n)
                .claveHash("$2a$10$pruebaDeEdicionSinClaveUtilizable0000000000000000000000")
                .activo(true)
                .build();

        entityManager.persist(editor);
        entityManager.flush();
    }

    // ---------- PUBLICADA -> EN_REVISION ----------

    @Test
    void editarPorPut_unaFichaPublicada_laDevuelveARevisionYLimpiaLosCuatroCampos() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        mockMvc.perform(put("/api/especies/" + ficha.id())
                        // La ficha esta publicada: hay que aceptar que sale del
                        // catalogo. Sin esto responde 409 y no edita nada, que es
                        // lo que fija ConfirmacionAlEditarPublicadaTest.
                        .param("confirmar", "true")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPut(ficha, "Descripción corregida tras publicar.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPublicacion").value("EN_REVISION"))
                .andExpect(jsonPath("$.validadaPor").doesNotExist())
                .andExpect(jsonPath("$.publicadaPor").doesNotExist());

        Especie guardada = recargar(ficha.id());
        assertEquals(EstadoPublicacion.EN_REVISION, guardada.getEstadoPublicacion());
        assertNull(guardada.getValidadaPor(), "quedo el validador");
        assertNull(guardada.getFechaValidacion(), "quedo la fecha de validacion");
        assertNull(guardada.getPublicadaPor(), "quedo el publicador");
        assertNull(guardada.getFechaPublicacion(), "quedo la fecha de publicacion");

        // El contenido nuevo si se guardo: invalidar la revision no es rechazar
        // el cambio.
        assertEquals("Descripción corregida tras publicar.", guardada.getDescripcion());

        // Y quien la creo no cambia porque alguien la edite.
        assertNotNull(guardada.getCreadaPor(), "se perdio el creador");
    }

    @Test
    void editarLaTaxonomia_deUnaFichaPublicada_laDevuelveARevision() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/taxonomia")
                        .param("confirmar", "true")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoTaxonomia(ficha, "Corregidaceae")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPublicacion").value("EN_REVISION"))
                .andExpect(jsonPath("$.validadaPor").doesNotExist())
                .andExpect(jsonPath("$.publicadaPor").doesNotExist())
                .andExpect(jsonPath("$.taxonomia.familia").value("Corregidaceae"));

        Especie guardada = recargar(ficha.id());
        assertEquals(EstadoPublicacion.EN_REVISION, guardada.getEstadoPublicacion());
        assertNull(guardada.getValidadaPor(), "quedo el validador");
        assertNull(guardada.getPublicadaPor(), "quedo el publicador");
    }

    /**
     * El efecto donde importa: no es solo que cambie una columna, es que la
     * ficha deja de estar en el catalogo que ve cualquiera.
     */
    @Test
    void editarUnaPublicada_laSacaDelCatalogoPublico() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        // Antes de editar: esta en el catalogo publico.
        assertTrue(estaEnPublicadas(ficha.id()),
                "la ficha sembrada como PUBLICADA no aparecia en /publicadas; "
                        + "el resto de la prueba no demostraria nada");

        mockMvc.perform(put("/api/especies/" + ficha.id())
                        .param("confirmar", "true")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPut(ficha, "Contenido que nadie ha revisado todavía.")))
                .andExpect(status().isOk());

        // Despues: fuera.
        assertTrue(!estaEnPublicadas(ficha.id()),
                "la ficha sigue en el catálogo público con contenido sin revisar");

        // Y su detalle publico tampoco responde.
        mockMvc.perform(get("/api/especies/publicadas/" + ficha.id()))
                .andExpect(status().isNotFound());
    }

    // ---------- EN_REVISION ya validada ----------

    @Test
    void editar_unaFichaYaValidada_leQuitaLaValidacionYSigueEnRevision() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.EN_REVISION);
        validar(ficha);
        assertNotNull(recargar(ficha.id()).getValidadaPor(), "la ficha no quedo validada");

        mockMvc.perform(put("/api/especies/" + ficha.id())
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPut(ficha, "Cambio después de validar.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPublicacion").value("EN_REVISION"))
                .andExpect(jsonPath("$.validadaPor").doesNotExist());

        Especie guardada = recargar(ficha.id());
        assertEquals(EstadoPublicacion.EN_REVISION, guardada.getEstadoPublicacion(),
                "el estado se movio; ya estaba EN_REVISION");
        assertNull(guardada.getValidadaPor(), "quedo el validador");
        assertNull(guardada.getFechaValidacion(), "quedo la fecha de validacion");
    }

    @Test
    void publicarSinRevalidar_despuesDeEditar_devuelve409() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.EN_REVISION);
        validar(ficha);
        editar(ficha, "Cambio que obliga a revalidar.");

        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/publicar")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_PUBLICAR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje")
                        .value("La especie debe ser validada antes de publicarse"));

        assertEquals(EstadoPublicacion.EN_REVISION, recargar(ficha.id()).getEstadoPublicacion());
    }

    // ---------- Los estados sin revision previa ----------

    @Test
    void editar_unaFichaEnBorrador_noTocaNingunCampoDeControl() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.BORRADOR);

        mockMvc.perform(put("/api/especies/" + ficha.id())
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPut(ficha, "Borrador en progreso.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPublicacion").value("BORRADOR"));

        Especie guardada = recargar(ficha.id());
        assertEquals(EstadoPublicacion.BORRADOR, guardada.getEstadoPublicacion());
        assertNull(guardada.getValidadaPor());
        assertNull(guardada.getPublicadaPor());
    }

    @Test
    void editar_unaFichaRechazada_sigueRechazada() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.RECHAZADA);

        mockMvc.perform(put("/api/especies/" + ficha.id())
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPut(ficha, "Corrigiendo lo que se rechazó.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPublicacion").value("RECHAZADA"));

        assertEquals(EstadoPublicacion.RECHAZADA, recargar(ficha.id()).getEstadoPublicacion());
    }

    @Test
    void editar_unaEnRevisionSinValidar_sigueIgual() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.EN_REVISION);

        mockMvc.perform(put("/api/especies/" + ficha.id())
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPut(ficha, "Corrección durante la revisión.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPublicacion").value("EN_REVISION"));

        Especie guardada = recargar(ficha.id());
        assertEquals(EstadoPublicacion.EN_REVISION, guardada.getEstadoPublicacion());
        assertNull(guardada.getValidadaPor());
    }

    // ---------- El permiso de la edicion ----------

    /**
     * La edicion sigue siendo {@code ESPECIE_EDITAR}: lo que cambio es la
     * consecuencia, no quien puede editar. Y si el guard rechaza, la
     * consecuencia no se aplica: la ficha no se mueve.
     * <p>
     * Hace falta aqui porque {@code AutorizacionPorPermisoTest} comprueba
     * {@code ESPECIE_EDITAR} a traves de {@code PATCH /{id}/activar}, no de
     * estos dos endpoints, que son los que ahora mueven el estado.
     */
    @Test
    void editar_sinEspecieEditar_devuelve403YNoMueveLaFicha() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        // Un token con permisos del flujo editorial, pero sin el de editar.
        String sinEditar = token(ESPECIE_CONSULTAR, ESPECIE_VALIDAR, ESPECIE_PUBLICAR);

        mockMvc.perform(put("/api/especies/" + ficha.id())
                        .header(HttpHeaders.AUTHORIZATION, sinEditar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPut(ficha, "Cambio que no debería entrar.")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/taxonomia")
                        .header(HttpHeaders.AUTHORIZATION, sinEditar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoTaxonomia(ficha, "Noentraceae")))
                .andExpect(status().isForbidden());

        Especie sinTocar = recargar(ficha.id());
        assertEquals(EstadoPublicacion.PUBLICADA, sinTocar.getEstadoPublicacion(),
                "un 403 movio el estado de la ficha");
        assertNotNull(sinTocar.getValidadaPor(), "un 403 limpio el validador");
        assertNotNull(sinTocar.getPublicadaPor(), "un 403 limpio el publicador");
    }

    @Test
    void editar_anonimo_devuelve401YNoMueveLaFicha() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        mockMvc.perform(put("/api/especies/" + ficha.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPut(ficha, "Cambio anónimo.")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/taxonomia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoTaxonomia(ficha, "Anonimaceae")))
                .andExpect(status().isUnauthorized());

        Especie sinTocar = recargar(ficha.id());
        assertEquals(EstadoPublicacion.PUBLICADA, sinTocar.getEstadoPublicacion(),
                "una peticion anonima movio el estado");
        assertNotNull(sinTocar.getPublicadaPor(), "una peticion anonima limpio el publicador");
    }

    // ---------- El ciclo entero ----------

    /**
     * La vuelta completa. Una regla que invalide la revision pero deje la ficha
     * sin camino de vuelta al catalogo seria tan inservible como no tenerla.
     */
    @Test
    void cicloCompleto_publicadaEditarRevalidarRepublicar() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        // 1. Editar: sale del catalogo.
        editar(ficha, "Contenido nuevo del ciclo completo.");
        assertEquals(EstadoPublicacion.EN_REVISION, recargar(ficha.id()).getEstadoPublicacion());
        assertTrue(!estaEnPublicadas(ficha.id()), "sigue publicada tras editar");

        // 2. Revalidar.
        validar(ficha);
        assertNotNull(recargar(ficha.id()).getValidadaPor(), "no se revalido");

        // 3. Volver a publicar.
        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/publicar")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_PUBLICAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPublicacion").value("PUBLICADA"));

        // 4. Y vuelve al catalogo, con el contenido nuevo.
        Especie guardada = recargar(ficha.id());
        assertEquals(EstadoPublicacion.PUBLICADA, guardada.getEstadoPublicacion());
        assertEquals("Contenido nuevo del ciclo completo.", guardada.getDescripcion());
        assertNotNull(guardada.getPublicadaPor(), "no quedo el publicador");
        assertTrue(estaEnPublicadas(ficha.id()), "no volvio al catálogo público");
    }

    // ---------- El limite del alcance ----------

    /**
     * Las ediciones de sub-recursos quedan fuera a proposito: es un cambio
     * aparte. Esta prueba fija el limite, para que ampliarlo sea una decision y
     * no un efecto colateral.
     */
    @Test
    void editarUnSubRecurso_noInvalidaLaRevision() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        // Una edicion de sub-recurso que de verdad funciona: el nombre comun de
        // la ficha, con el mismo ESPECIE_EDITAR. Tiene que dar 200, porque una
        // prueba que se apoyara en un 4xx no distinguiria "no invalida" de
        // "la peticion ni llego".
        mockMvc.perform(put("/api/nombres-comunes/" + ficha.nombreComun().getIdNombreComun())
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Nombre común corregido",
                                  "region": "Prueba",
                                  "esPrincipal": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nombre común corregido"));

        // Y la ficha no se movio: ningun endpoint de sub-recurso pasa por
        // invalidarRevisionPrevia.
        Especie sinTocar = recargar(ficha.id());
        assertEquals(EstadoPublicacion.PUBLICADA, sinTocar.getEstadoPublicacion(),
                "un sub-recurso invalido la revision; el alcance cambio");
        assertNotNull(sinTocar.getValidadaPor(), "un sub-recurso limpio el validador");
        assertNotNull(sinTocar.getPublicadaPor(), "un sub-recurso limpio el publicador");
    }

    // ---------- Regresion del hueco de familia ----------

    /**
     * El hueco del trabajo anterior, visto desde esta regla. Antes:
     * validar con familia, borrarla por PATCH, publicar. La compuerta de
     * {@code publicar} lo cortaba con un 409; ahora el PATCH ademas quita la
     * validacion, asi que la ficha ni llega a estar en condiciones de
     * publicarse. Las dos defensas se sostienen, y esta prueba comprueba las
     * dos cosas por separado: que la validacion se fue, y que si alguien
     * revalida, la compuerta de familia sigue ahi.
     */
    @Test
    void regresionDelHuecoDeFamilia_editarQuitaLaValidacionYLaCompuertaSigueCubriendo()
            throws Exception {

        Ficha ficha = sembrar(EstadoPublicacion.EN_REVISION);
        validar(ficha);

        // Borrar la familia por PATCH /taxonomia.
        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/taxonomia")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoTaxonomia(ficha, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxonomia.familia").doesNotExist());

        // 1. La edicion le quito la validacion.
        assertNull(recargar(ficha.id()).getValidadaPor(),
                "la ficha sigue validada despues de editarle la taxonomia");

        // 2. Publicar falla por falta de validacion, no por la familia.
        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/publicar")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_PUBLICAR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje")
                        .value("La especie debe ser validada antes de publicarse"));

        // 3. Y si alguien intenta revalidar, la compuerta de familia sigue ahi.
        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/validar")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_VALIDAR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje")
                        .value("No se puede validar: la familia de la taxonomía es obligatoria"));

        Especie sinTocar = recargar(ficha.id());
        assertEquals(EstadoPublicacion.EN_REVISION, sinTocar.getEstadoPublicacion());
        assertNull(sinTocar.getPublicadaPor(), "quedo publicada sin familia");
    }

    // ---------- Apoyo ----------

    private Ficha sembrar(EstadoPublicacion estado) {
        return new SiembraDeFichas(entityManager).sembrar(estado);
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

    private void validar(Ficha ficha) throws Exception {
        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/validar")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_VALIDAR)))
                .andExpect(status().isOk());
    }

    /**
     * Edita, sea cual sea el estado. Manda siempre {@code confirmar=true}: en
     * una ficha publicada hace falta, y en los demas estados se ignora, asi que
     * el ayudante sirve igual para todos. Que sin confirmar NO se edite es otra
     * cosa, y la fija {@code ConfirmacionAlEditarPublicadaTest}.
     */
    private void editar(Ficha ficha, String descripcion) throws Exception {
        mockMvc.perform(put("/api/especies/" + ficha.id())
                        .param("confirmar", "true")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPut(ficha, descripcion)))
                .andExpect(status().isOk());
    }

    /** {@code GET /api/especies/publicadas} trae la ficha? */
    private boolean estaEnPublicadas(Long id) throws Exception {
        String cuerpo = mockMvc.perform(get("/api/especies/publicadas"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return cuerpo.contains("\"idEspecie\":" + id + ",");
    }

    /**
     * Cuerpo del PUT: se conservan el nombre cientifico y la taxonomia de la
     * ficha --son unicos en el esquema y cambiarlos daria un 409 de duplicado
     * que no es lo que se mide-- y solo se mueve la descripcion.
     */
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

    /** Cuerpo del PATCH /taxonomia; {@code familia} null lo omite del JSON. */
    private static String cuerpoTaxonomia(Ficha ficha, String familia) {
        String campoFamilia = familia == null ? "" : ",\n  \"familia\": \"" + familia + "\"";

        return """
                {
                  "genero": "%s",
                  "especieTaxonomica": "%s"%s
                }
                """.formatted(
                ficha.especie().getTaxonomia().getGenero(),
                ficha.especie().getTaxonomia().getEspecieTaxonomica(),
                campoFamilia);
    }

    private Especie recargar(Long id) {
        entityManager.flush();
        entityManager.clear();
        return especieRepository.findById(id).orElseThrow();
    }
}
