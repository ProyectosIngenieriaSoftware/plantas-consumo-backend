package sv.edu.ues.fmp.flora.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import sv.edu.ues.fmp.flora.config.CarpetaDeImagenesDePrueba;
import sv.edu.ues.fmp.flora.config.ImagenesDePrueba;
import sv.edu.ues.fmp.flora.dto.response.UsuarioResponse;
import sv.edu.ues.fmp.flora.entity.enums.EstadoPublicacion;

/**
 * Cualquier cambio de imagen refresca {@code fecha_actualizacion} de la especie
 * dueña. Segunda mitad de la observación de la revisión.
 *
 * <h2>Por que esta clase NO es @Transactional, a diferencia de las demas</h2>
 * El refresco lo hace el trigger {@code trg_imagen_especie_toca_especie} (V23)
 * con un UPDATE sobre {@code especie}, y la fecha la escribe el BEFORE UPDATE
 * que ya existia, con {@code CURRENT_TIMESTAMP}. Y en PostgreSQL
 * {@code CURRENT_TIMESTAMP} es el instante de inicio de la
 * <strong>transaccion</strong>, no del statement.
 * <p>
 * O sea que dentro de una sola transaccion la fecha no se mueve: una prueba
 * {@code @Transactional} que sembrara la ficha y subiera la imagen en la misma
 * transaccion leeria antes y despues el mismo valor, y no distinguiria "el
 * trigger funciona" de "el trigger no existe". Para medirlo hacen falta
 * transacciones distintas, y por tanto commits de verdad.
 * <p>
 * De ahi lo demas: la ficha se crea por SQL con un nombre centinela
 * ({@code Zzfecha…}) y {@link #limpiar()} borra todo lo que se creo, igual que
 * {@code EspecieClasificacionConcurrenciaTest}, que no puede ser transaccional
 * por el mismo tipo de razon.
 *
 * <h2>Por que la ficha nace con la fecha una hora atras</h2>
 * {@code fecha_actualizacion} tiene DEFAULT {@code CURRENT_TIMESTAMP} y el
 * trigger de {@code especie} es BEFORE <em>UPDATE</em>, asi que en un INSERT se
 * puede dar un valor explicito y el trigger no lo pisa. Poniendolo una hora
 * atras, la comprobacion no depende de la resolucion del reloj ni de cuanto
 * tarde la peticion: cualquier toque da un valor estrictamente mayor. Sin eso,
 * dos transacciones consecutivas podrian --en teoria-- compartir marca de
 * tiempo y la prueba seria intermitente.
 *
 * <h2>Lo que se mide en cada caso</h2>
 * Las cuatro operaciones que cambian las imagenes de una ficha: subir un
 * archivo, registrar un enlace, dar de baja y reactivar. Las cuatro tienen que
 * mover la fecha, y el enlace tanto como el archivo: la regla es "cambio de
 * imagen", no "cambio de archivo".
 * <p>
 * Y lo que <strong>no</strong> tiene que cambiar: el estado. Refrescar la fecha
 * no es editar la ficha, asi que una PUBLICADA sigue PUBLICADA y sigue en el
 * catalogo. Lo comprueba
 * {@link #elRefrescoNoCambiaElEstado_laPublicadaSigueEnElCatalogo()}, que es la
 * prueba de que el trigger no se parece a {@code invalidarRevisionPrevia}.
 *
 * <h2>Residuo</h2>
 * {@link #limpiar()} borra las imagenes, la especie y la taxonomia de prueba, y
 * vacia la carpeta temporal. No se toca ninguna de las fichas reales del
 * catalogo <strong>a proposito</strong>: subirles una imagen les cambiaria la
 * fecha, y eso es un cambio real que ningun borrado posterior revierte.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ImagenRefrescaFechaDeEspecieTest {

    private static final String MULTIMEDIA = "MULTIMEDIA_GESTIONAR";

    /** Centinela: nada del catalogo real empieza por "Zzfecha". */
    private static final String GENERO = "Zzfecha";
    private static final String PREFIJO_NOMBRE = "Zzfecha ";

    private static final AtomicLong SECUENCIA = new AtomicLong();

    @DynamicPropertySource
    static void carpetaDeImagenes(DynamicPropertyRegistry registro) {
        CarpetaDeImagenesDePrueba.registrar(registro);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailService emailService;

    private Long idUsuario;

    @BeforeEach
    void preparar() {
        idUsuario = jdbcTemplate.queryForObject(
                "SELECT min(id_usuario) FROM usuario WHERE activo = true", Long.class);
        assertNotNull(idUsuario, "no hay ninguna cuenta activa para firmar los tokens");

        CarpetaDeImagenesDePrueba.limpiar();
    }

    /**
     * Borra en orden de dependencia. Las imagenes primero: su DELETE dispara el
     * trigger, que actualiza una especie que un momento despues se borra
     * tambien, asi que el toque final no deja nada.
     */
    @AfterEach
    void limpiar() {
        jdbcTemplate.update("""
                DELETE FROM imagen_especie
                WHERE id_especie IN (SELECT id_especie FROM especie
                                     WHERE nombre_cientifico LIKE ?)
                """, PREFIJO_NOMBRE + "%");
        jdbcTemplate.update("DELETE FROM especie WHERE nombre_cientifico LIKE ?",
                PREFIJO_NOMBRE + "%");
        jdbcTemplate.update("DELETE FROM taxonomia WHERE genero = ?", GENERO);

        CarpetaDeImagenesDePrueba.limpiar();
    }

    // ---------- Subir un archivo ----------

    @Test
    void subirUnaImagenPropia_refrescaLaFechaDeLaEspecie() throws Exception {
        long idEspecie = crearFicha(EstadoPublicacion.PUBLICADA);
        Timestamp antes = fecha(idEspecie);

        subirArchivo(idEspecie);

        Timestamp despues = fecha(idEspecie);
        assertTrue(despues.after(antes),
                "la fecha no se movio al subir una imagen: antes=" + antes
                        + " despues=" + despues);
    }

    // ---------- Registrar un enlace: la regla no es solo para archivos ----------

    @Test
    void registrarUnaImagenPorUrl_refrescaLaFechaDeLaEspecie() throws Exception {
        long idEspecie = crearFicha(EstadoPublicacion.PUBLICADA);
        Timestamp antes = fecha(idEspecie);

        registrarPorUrl(idEspecie);

        Timestamp despues = fecha(idEspecie);
        assertTrue(despues.after(antes),
                "la fecha no se movio al registrar un enlace: antes=" + antes
                        + " despues=" + despues);
    }

    // ---------- Baja y alta ----------

    @Test
    void laBajaLogicaDeUnaImagenPropia_refrescaLaFechaDeLaEspecie() throws Exception {
        long idEspecie = crearFicha(EstadoPublicacion.PUBLICADA);
        long idImagen = subirArchivo(idEspecie);

        // Se vuelve a poner atras para medir la baja y no la subida anterior.
        retrasarFecha(idEspecie);
        Timestamp antes = fecha(idEspecie);

        mockMvc.perform(delete("/api/imagenes-especie/" + idImagen)
                        .header(HttpHeaders.AUTHORIZATION, token(MULTIMEDIA)))
                .andExpect(status().isNoContent());

        Timestamp despues = fecha(idEspecie);
        assertTrue(despues.after(antes),
                "la fecha no se movio al dar de baja la imagen: antes=" + antes
                        + " despues=" + despues);

        assertFalse(jdbcTemplate.queryForObject(
                        "SELECT activa FROM imagen_especie WHERE id_imagen = ?",
                        Boolean.class, idImagen),
                "la imagen no quedo inactiva");
    }

    @Test
    void laBajaLogicaDeUnaImagenPorUrl_tambienRefrescaLaFecha() throws Exception {
        long idEspecie = crearFicha(EstadoPublicacion.PUBLICADA);
        long idImagen = registrarPorUrl(idEspecie);

        retrasarFecha(idEspecie);
        Timestamp antes = fecha(idEspecie);

        mockMvc.perform(delete("/api/imagenes-especie/" + idImagen)
                        .header(HttpHeaders.AUTHORIZATION, token(MULTIMEDIA)))
                .andExpect(status().isNoContent());

        assertTrue(fecha(idEspecie).after(antes),
                "la fecha no se movio al dar de baja un enlace");
    }

    @Test
    void reactivarUnaImagen_refrescaLaFechaDeLaEspecie() throws Exception {
        long idEspecie = crearFicha(EstadoPublicacion.PUBLICADA);
        long idImagen = subirArchivo(idEspecie);

        mockMvc.perform(delete("/api/imagenes-especie/" + idImagen)
                        .header(HttpHeaders.AUTHORIZATION, token(MULTIMEDIA)))
                .andExpect(status().isNoContent());

        retrasarFecha(idEspecie);
        Timestamp antes = fecha(idEspecie);

        mockMvc.perform(patch("/api/imagenes-especie/" + idImagen + "/activar")
                        .header(HttpHeaders.AUTHORIZATION, token(MULTIMEDIA)))
                .andExpect(status().isOk());

        assertTrue(fecha(idEspecie).after(antes),
                "la fecha no se movio al reactivar la imagen");
    }

    // ---------- Lo que el refresco NO hace ----------

    /**
     * La diferencia con editar la ficha. Editar una PUBLICADA la saca del
     * catalogo y la devuelve a revision ({@code invalidarRevisionPrevia}); esto
     * no: el trigger escribe una sola columna y no toca el estado, el validador
     * ni el publicador. Una ficha publicada a la que se le cambian las imagenes
     * sigue publicada.
     */
    @Test
    void elRefrescoNoCambiaElEstado_laPublicadaSigueEnElCatalogo() throws Exception {
        long idEspecie = crearFicha(EstadoPublicacion.PUBLICADA);
        Timestamp antes = fecha(idEspecie);

        long idImagen = subirArchivo(idEspecie);

        mockMvc.perform(delete("/api/imagenes-especie/" + idImagen)
                        .header(HttpHeaders.AUTHORIZATION, token(MULTIMEDIA)))
                .andExpect(status().isNoContent());

        // La fecha si se movio: las dos operaciones tocaron la ficha.
        assertTrue(fecha(idEspecie).after(antes), "la fecha no se movio");

        // Y nada mas se movio.
        assertEquals("PUBLICADA", jdbcTemplate.queryForObject(
                "SELECT estado_publicacion FROM especie WHERE id_especie = ?",
                String.class, idEspecie),
                "el refresco cambio el estado de publicacion");
        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT validada_por FROM especie WHERE id_especie = ?",
                Long.class, idEspecie),
                "el refresco limpio el validador");
        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT publicada_por FROM especie WHERE id_especie = ?",
                Long.class, idEspecie),
                "el refresco limpio el publicador");

        // Y sigue en el catalogo publico.
        mockMvc.perform(get("/api/especies/publicadas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].idEspecie")
                        .value(Matchers.hasItem((int) idEspecie)));
    }

    // ---------- Apoyo ----------

    /**
     * Crea la ficha de prueba por SQL, confirmada, con la fecha una hora atras.
     * <p>
     * Por SQL y no por el POST de especie porque hace falta nacer en el estado
     * exacto --PUBLICADA necesita validador, publicador y las dos fechas, que
     * los CHECK exigen juntos-- y con una fecha controlada.
     */
    private long crearFicha(EstadoPublicacion estado) {
        long n = SECUENCIA.incrementAndGet();

        Long idTaxonomia = jdbcTemplate.queryForObject("""
                INSERT INTO taxonomia (reino, division, clase, orden_taxonomico,
                                       familia, genero, especie_taxonomica)
                VALUES ('Plantae', 'Zzphyta', 'Zzopsida', 'Zzales',
                        'Zzaceae', ?, ?)
                RETURNING id_taxonomia
                """, Long.class, GENERO, "fecha" + n);

        boolean publicada = estado == EstadoPublicacion.PUBLICADA;

        return jdbcTemplate.queryForObject("""
                INSERT INTO especie (id_taxonomia, nombre_cientifico, descripcion,
                                     estado_publicacion, activa, creada_por,
                                     validada_por, fecha_validacion,
                                     publicada_por, fecha_publicacion,
                                     fecha_registro, fecha_actualizacion)
                VALUES (?, ?, 'Ficha de prueba del refresco de fecha por imagenes.',
                        ?, true, ?,
                        CASE WHEN ? THEN ? END, CASE WHEN ? THEN CURRENT_TIMESTAMP END,
                        CASE WHEN ? THEN ? END, CASE WHEN ? THEN CURRENT_TIMESTAMP END,
                        CURRENT_TIMESTAMP - interval '1 hour',
                        CURRENT_TIMESTAMP - interval '1 hour')
                RETURNING id_especie
                """, Long.class,
                idTaxonomia, PREFIJO_NOMBRE + "fecha" + n, estado.name(), idUsuario,
                publicada, idUsuario, publicada,
                publicada, idUsuario, publicada);
    }

    /**
     * Vuelve a poner la fecha una hora atras para medir la siguiente
     * operacion.
     * <p>
     * No se puede con un UPDATE normal: el BEFORE UPDATE de {@code especie} la
     * pisaria con CURRENT_TIMESTAMP, que es justo su trabajo. Se desactiva el
     * trigger el tiempo justo de escribir el valor. Es una maniobra de prueba
     * --nada del codigo de produccion hace esto-- y vale la pena porque la
     * alternativa, esperar un segundo entre operaciones, haria la suite mas
     * lenta y seguiria dependiendo del reloj.
     */
    private void retrasarFecha(long idEspecie) {
        jdbcTemplate.execute("ALTER TABLE especie DISABLE TRIGGER trg_especie_fecha_actualizacion");
        try {
            jdbcTemplate.update("""
                    UPDATE especie
                    SET fecha_actualizacion = CURRENT_TIMESTAMP - interval '1 hour'
                    WHERE id_especie = ?
                    """, idEspecie);
        } finally {
            jdbcTemplate.execute(
                    "ALTER TABLE especie ENABLE TRIGGER trg_especie_fecha_actualizacion");
        }
    }

    private Timestamp fecha(long idEspecie) {
        return jdbcTemplate.queryForObject(
                "SELECT fecha_actualizacion FROM especie WHERE id_especie = ?",
                Timestamp.class, idEspecie);
    }

    private long subirArchivo(long idEspecie) throws Exception {
        String cuerpo = mockMvc.perform(multipart("/api/especies/" + idEspecie
                        + "/imagenes/archivo")
                        .file(new MockMultipartFile("archivo", "planta.jpg", "image/jpeg",
                                ImagenesDePrueba.jpeg()))
                        .param("tipoImagen", "HOJA")
                        .param("textoAlternativo", "Imagen de prueba del refresco de fecha")
                        .header(HttpHeaders.AUTHORIZATION, token(MULTIMEDIA)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return idDe(cuerpo);
    }

    private long registrarPorUrl(long idEspecie) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/especies/" + idEspecie + "/imagenes")
                        .header(HttpHeaders.AUTHORIZATION, token(MULTIMEDIA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoImagen": "FLOR",
                                  "urlImagen": "https://example.invalid/zzfecha-%d.jpg",
                                  "textoAlternativo": "Enlace de prueba del refresco"
                                }
                                """.formatted(SECUENCIA.incrementAndGet())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return idDe(cuerpo);
    }

    private static long idDe(String cuerpoJson) {
        java.util.regex.Matcher coincidencia =
                java.util.regex.Pattern.compile("\"idImagen\":(\\d+)").matcher(cuerpoJson);
        assertTrue(coincidencia.find(), "la respuesta no trae idImagen: " + cuerpoJson);
        return Long.parseLong(coincidencia.group(1));
    }

    private String token(String... permisos) {
        UsuarioResponse usuario = UsuarioResponse.builder()
                .id(idUsuario)
                .nombres("Refresco")
                .apellidos("De Fecha")
                .correo("refresco.fecha@prueba.local")
                .nombreUsuario("refresco.fecha")
                .activo(true)
                .build();

        return "Bearer "
                + jwtService.emitir(usuario, List.of("CONSULTOR"), List.of(permisos)).token();
    }
}
