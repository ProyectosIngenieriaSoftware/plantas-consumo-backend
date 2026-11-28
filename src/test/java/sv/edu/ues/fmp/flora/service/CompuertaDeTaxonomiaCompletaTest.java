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
import java.util.function.Consumer;

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
import sv.edu.ues.fmp.flora.entity.Taxonomia;
import sv.edu.ues.fmp.flora.entity.Usuario;
import sv.edu.ues.fmp.flora.entity.enums.EstadoPublicacion;
import sv.edu.ues.fmp.flora.repository.EspecieRepository;

/**
 * La compuerta de taxonomia completa del flujo editorial: revision #6 del Dr.
 * Dagoberto. La clasificacion taxonomica tiene que estar completa para validar
 * y para publicar, y sigue siendo opcional mientras la ficha esta en BORRADOR
 * o EN_REVISION sin validar.
 *
 * <h2>De la compuerta de familia a esta</h2>
 * Esta clase es la que antes se llamaba {@code CompuertaDeFamiliaTest}, cuando
 * la regla exigia solo la familia. La regla crecio a los siete rangos
 * --{@code reino}, {@code division}, {@code clase}, {@code ordenTaxonomico},
 * {@code familia}, {@code genero}, {@code especieTaxonomica}-- y con ella el
 * mensaje del 409, que ahora nombra los que faltan. La familia no pierde
 * cobertura: es uno de los siete y conserva sus tres casos
 * ({@link #validar_sinFamilia_devuelve409YNoValida()},
 * {@link #validar_conLaFamiliaEnBlanco_devuelve409()} y
 * {@link #publicar_deUnaFichaValidadaSinFamilia_devuelve409YNoPublica()}), que
 * son los de antes con el mensaje nuevo.
 * <p>
 * {@code subespecie} no entra en la compuerta y por eso no se prueba su
 * ausencia: no es un rango que toda especie tenga. Lo que si se prueba es que
 * no estorba --las fichas de
 * {@link #validar_conLaTaxonomiaCompleta_fijaValidadaPorYDejaLaFichaEnRevision()}
 * y {@link #publicar_conLaTaxonomiaCompleta_pasaAPublicada()} la tienen en
 * null y pasan las dos transiciones--, que es la forma de las quince fichas
 * reales del catalogo.
 *
 * <h2>Por que son dos compuertas y no una</h2>
 * La de {@code validar} es la que cumple la regla editorial: el revisor no
 * aprueba una ficha cuya clasificacion este a medias. Pero por si sola no
 * garantiza nada sobre lo publicado, y ahi esta el hueco que cierra la
 * segunda: ni {@code actualizar} (PUT) ni {@code actualizarTaxonomia}
 * (PATCH /taxonomia) tienen guarda de estado, y
 * {@code TaxonomiaMapper.updateEntity} escribe cada rango tal cual, incluido el
 * null. Con solo la primera compuerta, la secuencia <em>validar completa
 * &rarr; vaciar un rango con un PATCH &rarr; publicar</em> dejaba una ficha
 * publicada con la taxonomia incompleta usando nada mas que
 * {@code ESPECIE_EDITAR} y los endpoints tal como estan documentados.
 * {@link #elHueco_validarVaciarLaTaxonomiaYPublicar_sigueSinPoderPublicarse()}
 * recorre esa secuencia entera por HTTP y fija el invariante que no debe
 * regresar: no publica la ficha.
 * <p>
 * Esa secuencia tiene hoy dos defensas encima. La regla de que editar invalida
 * la revision previa ({@code EdicionInvalidaLaRevisionTest}) quita la
 * validacion en el paso del PATCH, de modo que es la que se adelanta. La
 * compuerta de {@code publicar} sigue siendo la que cubre el caso que ninguna
 * edicion puede producir --una ficha validada con la taxonomia incompleta, o
 * sea datos anteriores a la regla-- y la fijan
 * {@link #publicar_deUnaFichaValidadaSinFamilia_devuelve409YNoPublica()} y
 * {@link #publicar_deUnaFichaValidadaSinDivision_devuelve409YNoPublica()}: si
 * alguien la quita, esas son las pruebas que se caen.
 *
 * <h2>El blanco y el null</h2>
 * La compuerta trata la cadena en blanco igual que el null. Por los endpoints
 * no se llega a guardar un {@code ""} --{@code Textos.normalizar} lo convierte
 * en null antes del mapper--, asi que los casos del blanco escriben
 * directamente en la entidad: es la forma de un dato que entro por SQL o por
 * una carga anterior a la regla, que es justamente de donde puede venir.
 *
 * <h2>Lo que esta clase NO cubre, porque ya esta cubierto</h2>
 * Los 403 por permiso insuficiente en las dos transiciones los fija
 * {@code AutorizacionPorPermisoTest}. Aqui se quedan los dos casos anonimos,
 * que alli no estaban.
 *
 * <h2>Residuo</h2>
 * {@code @Transactional}: el usuario, las fichas sembradas y las que se crean
 * por POST viven en la transaccion de la prueba y el rollback se los lleva. No
 * hay commit en ninguna parte.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CompuertaDeTaxonomiaCompletaTest {

    private static final String ESPECIE_CREAR = "ESPECIE_CREAR";
    private static final String ESPECIE_EDITAR = "ESPECIE_EDITAR";
    private static final String ESPECIE_VALIDAR = "ESPECIE_VALIDAR";
    private static final String ESPECIE_PUBLICAR = "ESPECIE_PUBLICAR";

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
                .apellidos("De Taxonomia")
                .correo("compuerta.taxonomia." + n + "@prueba.local")
                .nombreUsuario("compuerta.taxonomia." + n)
                // No es una clave usable: este usuario no inicia sesion, el
                // token se emite directamente con JwtService.
                .claveHash("$2a$10$pruebaDeCompuertaSinClaveUtilizable00000000000000000000")
                .activo(true)
                .build();

        entityManager.persist(editor);
        entityManager.flush();
    }

    // ---------- Compuerta en validar ----------

    /**
     * El camino bueno. La ficha sembrada trae los siete rangos y
     * {@code subespecie} en null, que es la forma de las fichas reales: la
     * compuerta la deja pasar, o sea que no exige la subespecie.
     */
    @Test
    void validar_conLaTaxonomiaCompleta_fijaValidadaPorYDejaLaFichaEnRevision() throws Exception {
        Ficha ficha = sembrarEnRevision();

        assertNull(ficha.especie().getTaxonomia().getSubespecie(),
                "la siembra trae subespecie; este caso ya no prueba que sea opcional");

        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/validar")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_VALIDAR)))
                .andExpect(status().isOk())
                // Validar no publica: el estado se mantiene a proposito.
                .andExpect(jsonPath("$.estadoPublicacion").value("EN_REVISION"))
                .andExpect(jsonPath("$.validadaPor").value("Compuerta De Taxonomia"));

        Especie guardada = recargar(ficha.id());
        assertNotNull(guardada.getValidadaPor(), "no quedo el validador");
        assertNotNull(guardada.getFechaValidacion(), "no quedo la fecha de validacion");
    }

    /** Regresion de la compuerta de familia, con el mensaje nuevo. */
    @Test
    void validar_sinFamilia_devuelve409YNoValida() throws Exception {
        Ficha ficha = sembrarEnRevision();
        cambiarTaxonomia(ficha, t -> t.setFamilia(null));

        rechazaValidar(ficha, "familia");

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
        cambiarTaxonomia(ficha, t -> t.setFamilia("   "));

        rechazaValidar(ficha, "familia");

        assertNull(recargar(ficha.id()).getValidadaPor(),
                "quedo validada con la familia en blanco");
    }

    @Test
    void validar_sinDivision_devuelve409YNoValida() throws Exception {
        Ficha ficha = sembrarEnRevision();
        cambiarTaxonomia(ficha, t -> t.setDivision(null));

        rechazaValidar(ficha, "division");

        assertNull(recargar(ficha.id()).getValidadaPor(), "quedo validada sin division");
    }

    @Test
    void validar_sinClase_devuelve409YNoValida() throws Exception {
        Ficha ficha = sembrarEnRevision();
        cambiarTaxonomia(ficha, t -> t.setClase(null));

        rechazaValidar(ficha, "clase");

        assertNull(recargar(ficha.id()).getValidadaPor(), "quedo validada sin clase");
    }

    @Test
    void validar_sinOrdenTaxonomico_devuelve409YNoValida() throws Exception {
        Ficha ficha = sembrarEnRevision();
        cambiarTaxonomia(ficha, t -> t.setOrdenTaxonomico(null));

        rechazaValidar(ficha, "ordenTaxonomico");

        assertNull(recargar(ficha.id()).getValidadaPor(), "quedo validada sin orden taxonomico");
    }

    /**
     * El mensaje nombra todos los que faltan, no el primero que encuentra: es
     * lo que le ahorra al usuario reenviar una vez por rango. En el orden de la
     * jerarquia, de reino a especie, que es como se lee una clasificacion.
     * <p>
     * El {@code genero} es NOT NULL en la base pero no rechaza el blanco, asi
     * que un espacio es la unica forma de que falte, y la compuerta lo cuenta
     * como ausente igual que los opcionales.
     */
    @Test
    void validar_conVariosRangosVacios_losNombraTodosEnOrdenDeJerarquia() throws Exception {
        Ficha ficha = sembrarEnRevision();
        cambiarTaxonomia(ficha, t -> {
            t.setDivision(null);
            t.setGenero("   ");
        });

        rechazaValidar(ficha, "division", "genero");

        assertNull(recargar(ficha.id()).getValidadaPor(), "quedo validada con dos rangos vacios");
    }

    /**
     * Los cuatro rangos que la base deja en null son los cuatro que de verdad
     * pueden faltar en una ficha creada por el POST, que no exige ninguno. Este
     * es el 409 que ve quien crea un borrador y lo manda a validar sin
     * completar la clasificacion.
     */
    @Test
    void validar_sinNingunoDeLosCuatroOpcionales_losNombraLosCuatro() throws Exception {
        Ficha ficha = sembrarEnRevision();
        cambiarTaxonomia(ficha, t -> {
            t.setDivision(null);
            t.setClase(null);
            t.setOrdenTaxonomico(null);
            t.setFamilia(null);
        });

        rechazaValidar(ficha, "division", "clase", "ordenTaxonomico", "familia");

        assertNull(recargar(ficha.id()).getValidadaPor(),
                "quedo validada sin ninguno de los cuatro rangos opcionales");
    }

    // ---------- Compuerta en publicar ----------

    @Test
    void publicar_conLaTaxonomiaCompleta_pasaAPublicada() throws Exception {
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
     * catalogo publico. Es la red de seguridad, y la razon de que la compuerta
     * este tambien en {@code publicar} y no solo en {@code validar}.
     */
    @Test
    void publicar_deUnaFichaValidadaSinFamilia_devuelve409YNoPublica() throws Exception {
        Ficha ficha = sembrarEnRevision();
        validarPorHttp(ficha);
        cambiarTaxonomia(ficha, t -> t.setFamilia(null));

        rechazaPublicar(ficha, "familia");

        Especie sinTocar = recargar(ficha.id());
        assertEquals(EstadoPublicacion.EN_REVISION, sinTocar.getEstadoPublicacion(),
                "se publico una ficha sin familia");
        assertNull(sinTocar.getPublicadaPor(), "quedo el publicador");
        assertNull(sinTocar.getFechaPublicacion(), "quedo la fecha de publicacion");
    }

    /** El mismo caso con un rango de los nuevos, para que la red cubra los siete. */
    @Test
    void publicar_deUnaFichaValidadaSinDivision_devuelve409YNoPublica() throws Exception {
        Ficha ficha = sembrarEnRevision();
        validarPorHttp(ficha);
        cambiarTaxonomia(ficha, t -> t.setDivision(null));

        rechazaPublicar(ficha, "division");

        Especie sinTocar = recargar(ficha.id());
        assertEquals(EstadoPublicacion.EN_REVISION, sinTocar.getEstadoPublicacion(),
                "se publico una ficha sin division");
        assertNull(sinTocar.getPublicadaPor(), "quedo el publicador");
    }

    // ---------- El hueco ----------

    /**
     * El hueco, recorrido entero por HTTP con permisos que cualquier editor
     * tiene y sin tocar la base a mano en ningun paso:
     * <ol>
     *   <li>La ficha se valida con la taxonomia completa: la compuerta de
     *       validar la deja pasar, como debe.</li>
     *   <li>Un PATCH /taxonomia que no trae los rangos opcionales los vacia.
     *       Devuelve 200: es legitimo, son opcionales para editar.</li>
     *   <li>Publicar: 409. Antes de estas reglas era un 200 y la ficha quedaba
     *       PUBLICADA con la clasificacion a medias.</li>
     * </ol>
     *
     * <h2>Cual de las dos defensas lo corta hoy</h2>
     * Desde que editar invalida la revision previa --ver
     * {@code EdicionInvalidaLaRevisionTest}--, el PATCH del paso 2 ademas
     * limpia {@code validadaPor}, asi que {@code publicar} falla una guarda
     * antes, por falta de validacion, y ese es el mensaje que se afirma aqui.
     * <p>
     * El mensaje no es lo que esta prueba protege: son dos defensas
     * independientes sobre el mismo camino, y la que se adelanto es la mas
     * restrictiva (no basta reponer los rangos, hay que revalidar). La
     * compuerta de taxonomia de {@code publicar} sigue cubierta por los dos
     * casos de arriba, que montan lo que solo pueden producir los datos viejos:
     * validada y con la clasificacion incompleta, sin pasar por ningun endpoint
     * de edicion.
     * <p>
     * Lo que esta prueba fija, y no debe regresar nunca, es el invariante: esta
     * secuencia no publica la ficha. Si algun dia da 200, hay un agujero.
     */
    @Test
    void elHueco_validarVaciarLaTaxonomiaYPublicar_sigueSinPoderPublicarse() throws Exception {
        Ficha ficha = sembrarEnRevision();

        // 1. Validar con la taxonomia completa: pasa.
        validarPorHttp(ficha);

        // 2. Vaciar los rangos opcionales con el PATCH de taxonomia. Es un 200
        // legitimo.
        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/taxonomia")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taxonomiaIncompleta(ficha)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxonomia.familia").doesNotExist())
                .andExpect(jsonPath("$.taxonomia.division").doesNotExist());

        // Que el paso 2 hizo lo que dice: si no, el 409 de abajo podria venir
        // de cualquier otra precondicion y la prueba no probaria nada.
        Especie editada = recargar(ficha.id());
        assertNull(editada.getTaxonomia().getFamilia(),
                "el PATCH no vacio la familia; el resto de la prueba no demuestra nada");
        assertNull(editada.getTaxonomia().getDivision(),
                "el PATCH no vacio la division; el resto de la prueba no demuestra nada");

        // Y que el paso 2 tambien le quito la validacion, que es la defensa que
        // se adelanta a la compuerta de taxonomia.
        assertNull(editada.getValidadaPor(), "la edicion no invalido la revision previa");

        // 3. Publicar: aqui se corta.
        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/publicar")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_PUBLICAR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje")
                        .value("La especie debe ser validada antes de publicarse"));

        Especie sinTocar = recargar(ficha.id());
        assertEquals(EstadoPublicacion.EN_REVISION, sinTocar.getEstadoPublicacion(),
                "la ficha llego a PUBLICADA con la taxonomia a medias: el hueco sigue abierto");
        assertNull(sinTocar.getPublicadaPor(), "quedo el publicador");
    }

    // ---------- La taxonomia sigue siendo opcional antes de validar ----------

    @Test
    void crear_conLaTaxonomiaIncompleta_sigueDevolviendo201() throws Exception {
        long n = SECUENCIA.incrementAndGet();

        mockMvc.perform(post("/api/especies")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_CREAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombreCientifico": "Zzcompuerta%d especie",
                                  "descripcion": "Ficha de prueba de la compuerta de taxonomía.",
                                  "taxonomia": {
                                    "genero": "Zzcompuerta%d",
                                    "especieTaxonomica": "sintaxonomia"
                                  }
                                }
                                """.formatted(n, n)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPublicacion").value("BORRADOR"))
                .andExpect(jsonPath("$.taxonomia.division").doesNotExist())
                .andExpect(jsonPath("$.taxonomia.clase").doesNotExist())
                .andExpect(jsonPath("$.taxonomia.ordenTaxonomico").doesNotExist())
                .andExpect(jsonPath("$.taxonomia.familia").doesNotExist());
    }

    @Test
    void editarLaTaxonomiaDeUnaFichaEnRevision_incompleta_sigueDevolviendo200() throws Exception {
        Ficha ficha = sembrarEnRevision();

        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/taxonomia")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taxonomiaIncompleta(ficha)))
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

    /** Ficha EN_REVISION con los siete rangos llenos y subespecie en null. */
    private Ficha sembrarEnRevision() {
        return new SiembraDeFichas(entityManager).sembrar(EstadoPublicacion.EN_REVISION);
    }

    /** El 409 esperado, nombrando los rangos que faltan en el orden del mensaje. */
    private static String mensajeEsperado(String accion, String... rangos) {
        return "No se puede " + accion + ": la clasificación taxonómica debe estar completa;"
                + " faltan: " + String.join(", ", rangos);
    }

    private void rechazaValidar(Ficha ficha, String... rangos) throws Exception {
        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/validar")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_VALIDAR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.error").value("Transición de estado no válida"))
                .andExpect(jsonPath("$.mensaje").value(mensajeEsperado("validar", rangos)));
    }

    private void rechazaPublicar(Ficha ficha, String... rangos) throws Exception {
        mockMvc.perform(patch("/api/especies/" + ficha.id() + "/publicar")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_PUBLICAR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje").value(mensajeEsperado("publicar", rangos)));
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
     * Escribe la taxonomia directamente en la entidad, para montar los estados
     * a los que los endpoints no dejan llegar: la ficha ya validada con la
     * clasificacion incompleta, y el rango en blanco. {@code flush()} porque
     * las consultas del controlador son SQL y no verian lo que siguiera en la
     * cola de Hibernate.
     */
    private void cambiarTaxonomia(Ficha ficha, Consumer<Taxonomia> cambio) {
        cambio.accept(ficha.especie().getTaxonomia());
        entityManager.flush();
    }

    /**
     * El cuerpo del PATCH /taxonomia sin ninguno de los cuatro rangos
     * opcionales, conservando el resto de la clasificacion sembrada:
     * {@code genero} y {@code especieTaxonomica} son obligatorios en el DTO, y
     * repetir los de la ficha evita chocar con {@code uk_taxonomia} --que se
     * excluye a si misma al actualizar-- y dejar la prueba contando un 409 de
     * duplicado como si fuera el de la compuerta.
     */
    private static String taxonomiaIncompleta(Ficha ficha) {
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
