package sv.edu.ues.fmp.flora.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * La compuerta de familia del flujo editorial: decision del equipo con
 * Agronomia, la familia de la taxonomia es obligatoria para validar y para
 * publicar, y opcional mientras la ficha esta en BORRADOR o EN_REVISION sin
 * validar.
 *
 * <h2>Por que son dos compuertas y no una</h2>
 * La de {@code validar} es la que cumple la regla editorial: el revisor no
 * aprueba una ficha que no diga a que familia pertenece. Pero por si sola no
 * garantiza nada sobre lo publicado, y ahi esta el hueco que cierra la
 * segunda: ni {@code actualizar} (PUT) ni {@code actualizarTaxonomia}
 * (PATCH /taxonomia) tenian guarda de estado, y
 * {@code TaxonomiaMapper.updateEntity} escribe {@code familia} tal cual,
 * incluido el null. Con solo la primera compuerta, la secuencia
 * <em>validar con familia &rarr; borrarla con un PATCH &rarr; publicar</em>
 * dejaba una ficha publicada sin familia usando nada mas que
 * {@code ESPECIE_EDITAR} y los endpoints tal como estan documentados.
 * {@link #elHueco_validarBorrarFamiliaYPublicar_sigueSinPoderPublicarse()}
 * recorre esa secuencia entera por HTTP y fija el invariante que no debe
 * regresar: no publica la ficha.
 * <p>
 * Esa secuencia tiene hoy dos defensas encima. Despues de esta clase se anadio
 * la regla de que editar invalida la revision previa
 * ({@code EdicionInvalidaLaRevisionTest}), que ademas quita la validacion en el
 * paso del PATCH, de modo que es la que se adelanta. La compuerta de
 * {@code publicar} sigue siendo la que cubre el caso que ninguna edicion puede
 * producir --una ficha validada y sin familia, o sea datos anteriores a la
 * regla-- y la fija
 * {@link #publicar_deUnaFichaValidadaSinFamilia_devuelve409YNoPublica()}: si
 * alguien la quita, esa es la prueba que se cae.
 *
 * <h2>El blanco y el null</h2>
 * La compuerta trata la cadena en blanco igual que el null. Por los endpoints
 * no se llega a guardar un {@code ""} --{@code Textos.normalizar} lo convierte
 * en null antes del mapper--, asi que
 * {@link #validar_conLaFamiliaEnBlanco_devuelve409()} escribe el blanco
 * directamente en la entidad: es la forma de un dato que entro por SQL o por
 * una carga anterior a la regla, que es justamente de donde puede venir.
 *
 * <h2>Lo que esta clase NO cubre, porque ya esta cubierto</h2>
 * Los 403 por permiso insuficiente en las dos transiciones los fija
 * {@code AutorizacionPorPermisoTest} (sin {@code ESPECIE_VALIDAR}, con solo
 * {@code ESPECIE_EDITAR}, sin {@code ESPECIE_PUBLICAR} y con solo
 * {@code ESPECIE_VALIDAR}). Aqui se anaden los dos casos anonimos, que alli no
 * estaban, para cerrar el cuadro del brief.
 *
 * <h2>Residuo</h2>
 * {@code @Transactional}: el usuario, las fichas sembradas y las que se crean
 * por POST viven en la transaccion de la prueba y el rollback se los lleva. No
 * hay commit en ninguna parte.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CompuertaDeFamiliaTest {

    private static final String ESPECIE_CREAR = "ESPECIE_CREAR";
    private static final String ESPECIE_EDITAR = "ESPECIE_EDITAR";
    private static final String ESPECIE_VALIDAR = "ESPECIE_VALIDAR";
    private static final String ESPECIE_PUBLICAR = "ESPECIE_PUBLICAR";

    private static final String MENSAJE_VALIDAR =
            "No se puede validar: la familia de la taxonomía es obligatoria";
    private static final String MENSAJE_PUBLICAR =
            "No se puede publicar: la familia de la taxonomía es obligatoria";

    /** Para que los nombres cientificos y los generos no choquen entre si. */
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
                .nombres("Compuerta")
                .apellidos("De Familia")
                .correo("compuerta.familia." + n + "@prueba.local")
                .nombreUsuario("compuerta.familia." + n)
                // No es una clave usable: este usuario no inicia sesion, el
                // token se emite directamente con JwtService.
                .claveHash("$2a$10$pruebaDeCompuertaSinClaveUtilizable00000000000000000000")
                .activo(true)
                .build();

        entityManager.persist(editor);
        entityManager.flush();
    }

    // ---------- Compuerta en validar ----------

    @Test
    void validar_conFamilia_fijaValidadaPorYDejaLaFichaEnRevision() throws Exception {
        Ficha ficha = sembrarEnRevision();

        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/validar")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_VALIDAR)))
                .andExpect(status().isOk())
                // Validar no publica: el estado se mantiene a proposito.
                .andExpect(jsonPath("$.estadoPublicacion").value("EN_REVISION"))
                .andExpect(jsonPath("$.validadaPor").value("Compuerta De Familia"));

        Especie guardada = recargar(ficha.id());
        assertNotNull(guardada.getValidadaPor(), "no quedo el validador");
        assertNotNull(guardada.getFechaValidacion(), "no quedo la fecha de validacion");
    }

    @Test
    void validar_sinFamilia_devuelve409YNoValida() throws Exception {
        Ficha ficha = sembrarEnRevision();
        ponerFamilia(ficha, null);

        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/validar")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_VALIDAR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_VALIDAR));

        Especie sinTocar = recargar(ficha.id());
        assertNull(sinTocar.getValidadaPor(), "quedo validada sin familia");
        assertNull(sinTocar.getFechaValidacion(), "quedo la fecha de validacion");
        assertEquals(EstadoPublicacion.EN_REVISION, sinTocar.getEstadoPublicacion(),
                "el 409 movio el estado");
    }

    /**
     * El blanco cuenta igual que el null. Por los endpoints no se llega a
     * guardar un {@code ""}, asi que se escribe en la entidad: es la forma que
     * tiene un dato cargado por SQL o anterior a la regla.
     */
    @Test
    void validar_conLaFamiliaEnBlanco_devuelve409() throws Exception {
        Ficha ficha = sembrarEnRevision();
        ponerFamilia(ficha, "   ");

        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/validar")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_VALIDAR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_VALIDAR));

        assertNull(recargar(ficha.id()).getValidadaPor(), "quedo validada con la familia en blanco");
    }

    // ---------- Compuerta en publicar ----------

    @Test
    void publicar_conFamilia_pasaAPublicada() throws Exception {
        Ficha ficha = sembrarEnRevision();
        validarPorHttp(ficha);

        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/publicar")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_PUBLICAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPublicacion").value("PUBLICADA"));

        Especie guardada = recargar(ficha.id());
        assertEquals(EstadoPublicacion.PUBLICADA, guardada.getEstadoPublicacion());
        assertNotNull(guardada.getPublicadaPor(), "no quedo el publicador");
    }

    /**
     * La ficha validada antes de que existiera la regla: tiene validador y no
     * tiene familia. La compuerta de publicar es lo unico que la separa del
     * catalogo publico.
     */
    @Test
    void publicar_deUnaFichaValidadaSinFamilia_devuelve409YNoPublica() throws Exception {
        Ficha ficha = sembrarEnRevision();
        validarPorHttp(ficha);
        ponerFamilia(ficha, null);

        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/publicar")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_PUBLICAR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_PUBLICAR));

        Especie sinTocar = recargar(ficha.id());
        assertEquals(EstadoPublicacion.EN_REVISION, sinTocar.getEstadoPublicacion(),
                "se publico una ficha sin familia");
        assertNull(sinTocar.getPublicadaPor(), "quedo el publicador");
        assertNull(sinTocar.getFechaPublicacion(), "quedo la fecha de publicacion");
    }

    // ---------- El hueco ----------

    /**
     * El hueco, recorrido entero por HTTP con permisos que cualquier editor
     * tiene y sin tocar la base a mano en ningun paso:
     * <ol>
     *   <li>La ficha se valida con familia: la compuerta de validar la deja
     *       pasar, como debe.</li>
     *   <li>Un PATCH /taxonomia sin el campo familia la borra. Devuelve 200:
     *       es legitimo, la familia es opcional para editar.</li>
     *   <li>Publicar: 409. Antes de las dos reglas era un 200 y la ficha
     *       quedaba PUBLICADA sin familia.</li>
     * </ol>
     *
     * <h2>Cual de las dos defensas lo corta hoy</h2>
     * Cuando se escribio esta prueba, la compuerta de {@code publicar} era la
     * unica que lo cortaba, y el 409 decia "No se puede publicar: la familia
     * ...". Desde que editar invalida la revision previa --ver
     * {@code EdicionInvalidaLaRevisionTest}--, el PATCH del paso 2 ademas
     * limpia {@code validadaPor}, asi que {@code publicar} ya falla una guarda
     * antes, por falta de validacion, y ese es el mensaje que se afirma aqui.
     * <p>
     * El cambio de mensaje no debilita nada: son dos defensas independientes
     * sobre el mismo camino, y la que se adelanto es la mas restrictiva (no
     * basta reponer la familia, hay que revalidar). La compuerta de familia de
     * {@code publicar} sigue cubierta por
     * {@link #publicar_deUnaFichaValidadaSinFamilia_devuelve409YNoPublica()},
     * que monta el caso que solo pueden producir los datos viejos: validada y
     * sin familia, sin pasar por ningun endpoint de edicion.
     * <p>
     * Lo que esta prueba fija, y no debe regresar nunca, es el invariante: esta
     * secuencia no publica la ficha. Si algun dia da 200, hay un agujero.
     */
    @Test
    void elHueco_validarBorrarFamiliaYPublicar_sigueSinPoderPublicarse() throws Exception {
        Ficha ficha = sembrarEnRevision();

        // 1. Validar con familia: pasa.
        validarPorHttp(ficha);

        // 2. Borrar la familia con el PATCH de taxonomia. Es un 200 legitimo.
        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/taxonomia")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taxonomiaSinFamilia(ficha)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxonomia.familia").doesNotExist());

        // Que el paso 2 hizo lo que dice: si no, el 409 de abajo podria venir
        // de cualquier otra precondicion y la prueba no probaria nada.
        assertNull(recargar(ficha.id()).getTaxonomia().getFamilia(),
                "el PATCH no borro la familia; el resto de la prueba no demuestra nada");

        // Y que el paso 2 tambien le quito la validacion, que es la defensa que
        // se adelanta a la compuerta de familia.
        assertNull(recargar(ficha.id()).getValidadaPor(),
                "la edicion no invalido la revision previa");

        // 3. Publicar: aqui se corta.
        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/publicar")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_PUBLICAR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje")
                        .value("La especie debe ser validada antes de publicarse"));

        Especie sinTocar = recargar(ficha.id());
        assertEquals(EstadoPublicacion.EN_REVISION, sinTocar.getEstadoPublicacion(),
                "la ficha llego a PUBLICADA sin familia: el hueco sigue abierto");
        assertNull(sinTocar.getPublicadaPor(), "quedo el publicador");
    }

    // ---------- La familia sigue siendo opcional antes de validar ----------

    @Test
    void crear_sinFamilia_sigueDevolviendo201() throws Exception {
        long n = SECUENCIA.incrementAndGet();

        mockMvc.perform(post("/api/especies")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_CREAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombreCientifico": "Zzcompuerta%d especie",
                                  "descripcion": "Ficha de prueba de la compuerta de familia.",
                                  "taxonomia": {
                                    "genero": "Zzcompuerta%d",
                                    "especieTaxonomica": "sinfamilia"
                                  }
                                }
                                """.formatted(n, n)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPublicacion").value("BORRADOR"))
                .andExpect(jsonPath("$.taxonomia.familia").doesNotExist());
    }

    @Test
    void editarLaTaxonomiaDeUnaFichaEnRevision_sinFamilia_sigueDevolviendo200() throws Exception {
        Ficha ficha = sembrarEnRevision();

        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/taxonomia")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taxonomiaSinFamilia(ficha)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPublicacion").value("EN_REVISION"))
                .andExpect(jsonPath("$.taxonomia.familia").doesNotExist());
    }

    // ---------- Los guards de permiso ----------

    @Test
    void validar_anonimo_devuelve401() throws Exception {
        Ficha ficha = sembrarEnRevision();

        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/validar"))
                .andExpect(status().isUnauthorized());

        assertNull(recargar(ficha.id()).getValidadaPor(), "una peticion anonima valido la ficha");
    }

    @Test
    void publicar_anonimo_devuelve401() throws Exception {
        Ficha ficha = sembrarEnRevision();
        validarPorHttp(ficha);

        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/publicar"))
                .andExpect(status().isUnauthorized());

        assertEquals(EstadoPublicacion.EN_REVISION, recargar(ficha.id()).getEstadoPublicacion(),
                "una peticion anonima publico la ficha");
    }

    // ---------- Apoyo ----------

    /** Ficha EN_REVISION con familia ("Pruebaceae", que pone la siembra). */
    private Ficha sembrarEnRevision() {
        return new SiembraDeFichas(entityManager).sembrar(EstadoPublicacion.EN_REVISION);
    }

    /** Token del editor, con los permisos que se le pasen. */
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

    private void validarPorHttp(Ficha ficha) throws Exception {
        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/validar")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_VALIDAR)))
                .andExpect(status().isOk());
    }

    /**
     * Escribe la familia directamente en la entidad, para montar los estados a
     * los que los endpoints no dejan llegar: la ficha ya validada sin familia y
     * la familia en blanco. {@code flush()} porque las consultas del
     * controlador son SQL y no verian lo que siguiera en la cola de Hibernate.
     */
    private void ponerFamilia(Ficha ficha, String familia) {
        ficha.especie().getTaxonomia().setFamilia(familia);
        entityManager.flush();
    }

    /**
     * El cuerpo del PATCH /taxonomia sin el campo familia, conservando el resto
     * de la clasificacion sembrada: {@code genero} y {@code especieTaxonomica}
     * son obligatorios en el DTO, y repetir los de la ficha evita chocar con
     * {@code uk_taxonomia} --que se excluye a si misma al actualizar-- y dejar
     * la prueba contando un 409 de duplicado como si fuera el de la compuerta.
     */
    private static String taxonomiaSinFamilia(Ficha ficha) {
        return """
                {
                  "genero": "%s",
                  "especieTaxonomica": "%s"
                }
                """.formatted(
                ficha.especie().getTaxonomia().getGenero(),
                ficha.especie().getTaxonomia().getEspecieTaxonomica());
    }

    private Especie recargar(Long id) {
        entityManager.flush();
        return especieRepository.findById(id).orElseThrow();
    }
}
