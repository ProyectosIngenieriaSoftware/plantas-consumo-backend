package sv.edu.ues.fmp.flora.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import sv.edu.ues.fmp.flora.config.CarpetaDeImagenesDePrueba;
import sv.edu.ues.fmp.flora.config.ImagenesDePrueba;
import sv.edu.ues.fmp.flora.config.SiembraDeFichas;
import sv.edu.ues.fmp.flora.config.SiembraDeFichas.Ficha;
import sv.edu.ues.fmp.flora.config.UsuariosDePrueba;
import sv.edu.ues.fmp.flora.dto.response.UsuarioResponse;
import sv.edu.ues.fmp.flora.entity.ImagenEspecie;
import sv.edu.ues.fmp.flora.entity.enums.EstadoPublicacion;
import sv.edu.ues.fmp.flora.entity.enums.OrigenImagen;
import sv.edu.ues.fmp.flora.repository.ImagenEspecieRepository;
import sv.edu.ues.fmp.flora.repository.UsuarioRepository;

/**
 * Subir imagenes propias: lo que se acepta, lo que se rechaza y que lo
 * rechazado no deje rastro.
 *
 * <h2>El cambio que cubre</h2>
 * Hasta V21 una imagen de especie solo podia ser un enlace externo. La revision
 * (Dr. Dagoberto / ing. Melba) pidio poder subir imagenes propias. El binario
 * va al sistema de archivos y en la base queda la referencia mas los
 * metadatos; el origen de cada fila lo dice {@link OrigenImagen}.
 *
 * <h2>Por que las pruebas del tipo son el centro de la clase</h2>
 * La extension del nombre y el {@code Content-Type} de la parte multipart los
 * escribe quien sube, y las dos cosas se cambian sin tocar el contenido. Si la
 * validacion se fiara de ellas, un {@code informe.pdf} renombrado a
 * {@code foto.jpg} entraria en la carpeta del servidor y despues se serviria
 * como {@code image/jpeg}. De ahi que haya un caso por cada forma de mentir:
 * texto renombrado, PDF renombrado, media firma de WebP, y un PNG de verdad
 * declarado como JPEG --este ultimo es el que distingue "detecto el formato" de
 * "comprobe que el cliente no se contradice"--.
 * <p>
 * Y en cada rechazo se comprueba que la carpeta siguiera vacia, porque un 400
 * que ya hubiera escrito el archivo seria un 400 inutil: el objetivo de validar
 * antes es que el disco del servidor no se toque.
 *
 * <h2>La carpeta</h2>
 * {@link CarpetaDeImagenesDePrueba} apunta {@code app.imagenes.directorio} a un
 * temporal del sistema y el {@code @AfterEach} lo vacia. Nada de esto escribe
 * en la carpeta real.
 *
 * <h2>Residuo</h2>
 * {@code @Transactional} para la base --las fichas sembradas y las filas de
 * imagen se van con el rollback-- y el vaciado de la carpeta para el disco. Los
 * archivos <strong>no</strong> los revierte la transaccion: por eso hacen falta
 * las dos cosas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImagenPropiaSubidaTest {

    private static final String MULTIMEDIA = "MULTIMEDIA_GESTIONAR";
    private static final String ESPECIE_CONSULTAR = "ESPECIE_CONSULTAR";

    private static final String RUTA_SUBIDA = "/api/especies/%d/imagenes/archivo";

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
    private EntityManager entityManager;

    @Autowired
    private ImagenEspecieRepository imagenEspecieRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private EmailService emailService;

    private Long idUsuario;

    @BeforeEach
    void usuario() {
        idUsuario = UsuariosDePrueba.idActivo(usuarioRepository);
        CarpetaDeImagenesDePrueba.limpiar();
    }

    @AfterEach
    void limpiarCarpeta() {
        CarpetaDeImagenesDePrueba.limpiar();
    }

    // ---------- Los tres formatos admitidos ----------

    @Test
    void subirJpeg_creaLaFilaPropiaElArchivoYSePuedeServir() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);
        byte[] contenido = ImagenesDePrueba.jpeg();

        String cuerpo = mockMvc.perform(subida(ficha, "planta.jpg", "image/jpeg", contenido))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origen").value("PROPIA"))
                // En una imagen propia no hay URL: lo exige ck_imagen_fuente_unica.
                .andExpect(jsonPath("$.urlImagen").doesNotExist())
                .andExpect(jsonPath("$.tipoContenido").value("image/jpeg"))
                .andExpect(jsonPath("$.nombreOriginal").value("planta.jpg"))
                .andExpect(jsonPath("$.tamanioBytes").value(contenido.length))
                .andExpect(jsonPath("$.activa").value(true))
                // El nombre del archivo en disco no se publica: es un detalle
                // del almacenamiento y el cliente pide la imagen por su id.
                .andExpect(jsonPath("$.rutaArchivo").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        Long idImagen = idDe(cuerpo);
        ImagenEspecie guardada = recargar(idImagen);

        assertEquals(OrigenImagen.PROPIA, guardada.getOrigen());
        assertNull(guardada.getUrlImagen(), "una imagen propia quedo con URL");
        assertNotNull(guardada.getRutaArchivo(), "no quedo el nombre del archivo");
        assertTrue(guardada.getRutaArchivo().endsWith(".jpg"),
                "el archivo no se guardo con la extension del formato detectado: "
                        + guardada.getRutaArchivo());
        assertTrue(CarpetaDeImagenesDePrueba.existe(guardada.getRutaArchivo()),
                "la fila quedo pero el archivo no se escribio");

        // Y se sirve: los mismos bytes y el tipo detectado.
        mockMvc.perform(get("/api/imagenes-especie/" + idImagen + "/archivo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"planta.jpg\""))
                .andExpect(content().bytes(contenido));
    }

    @Test
    void subirPng_seAceptaYSeSirveComoPng() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);
        byte[] contenido = ImagenesDePrueba.png();

        Long idImagen = idDe(mockMvc.perform(subida(ficha, "hoja.png", "image/png", contenido))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoContenido").value("image/png"))
                .andReturn().getResponse().getContentAsString());

        assertTrue(recargar(idImagen).getRutaArchivo().endsWith(".png"));

        mockMvc.perform(get("/api/imagenes-especie/" + idImagen + "/archivo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(contenido));
    }

    @Test
    void subirWebp_seAceptaYSeSirveComoWebp() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);
        byte[] contenido = ImagenesDePrueba.webp();

        Long idImagen = idDe(mockMvc.perform(subida(ficha, "fruto.webp", "image/webp", contenido))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoContenido").value("image/webp"))
                .andReturn().getResponse().getContentAsString());

        assertTrue(recargar(idImagen).getRutaArchivo().endsWith(".webp"));

        mockMvc.perform(get("/api/imagenes-especie/" + idImagen + "/archivo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.valueOf("image/webp")))
                .andExpect(content().bytes(contenido));
    }

    /**
     * El formato manda sobre la extension que trajo el cliente: un JPEG
     * llamado {@code .jpeg} se guarda igualmente como {@code .jpg}, para que en
     * la carpeta no convivan dos extensiones del mismo formato.
     */
    @Test
    void subirJpegConExtensionJpeg_seGuardaNormalizadaAJpg() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        Long idImagen = idDe(mockMvc.perform(
                        subida(ficha, "flor.jpeg", "image/jpeg", ImagenesDePrueba.jpeg()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        assertTrue(recargar(idImagen).getRutaArchivo().endsWith(".jpg"),
                "la extension no se normalizo");
    }

    // ---------- Lo que se rechaza, sin escribir nada ----------

    @Test
    void subirTextoRenombradoAJpg_devuelve400YNoEscribeNada() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        mockMvc.perform(subida(ficha, "foto.jpg", "image/jpeg", ImagenesDePrueba.texto()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje")
                        .value(Matchers.containsString("no es una imagen válida")));

        assertNadaEscrito();
        assertSinImagenes(ficha);
    }

    @Test
    void subirPdfRenombradoAJpg_devuelve400YNoEscribeNada() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        mockMvc.perform(subida(ficha, "informe.jpg", "image/jpeg", ImagenesDePrueba.pdf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value(Matchers.containsString("no es una imagen válida")));

        assertNadaEscrito();
        assertSinImagenes(ficha);
    }

    /**
     * Media firma no basta. Un WAV y un AVI empiezan igual que un WebP
     * ({@code "RIFF"}) y se distinguen por los cuatro bytes del byte 8.
     */
    @Test
    void subirRiffQueNoEsWebp_devuelve400() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        mockMvc.perform(subida(ficha, "sonido.webp", "image/webp",
                        ImagenesDePrueba.riffQueNoEsWebp()))
                .andExpect(status().isBadRequest());

        assertNadaEscrito();
    }

    /**
     * El caso que separa las dos comprobaciones: el archivo <em>si</em> es una
     * imagen admitida, pero no la que el cliente dice. Guardarlo en silencio
     * dejaria la fila con un {@code tipo_contenido} que no describe el archivo,
     * o --si se creyera al cliente-- un PNG servido como {@code image/jpeg}.
     */
    @Test
    void subirPngDeclarandoJpeg_devuelve400PorContradiccion() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        mockMvc.perform(subida(ficha, "confusa.jpg", "image/jpeg", ImagenesDePrueba.png()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value(Matchers.containsString("image/png")))
                .andExpect(jsonPath("$.mensaje")
                        .value(Matchers.containsString("no corresponde a ese formato")));

        assertNadaEscrito();
        assertSinImagenes(ficha);
    }

    /**
     * Un tipo declarado que no se reconoce no es una contradiccion: es lo que
     * manda un navegador cuando no sabe, y rechazarlo seria rechazar subidas
     * legitimas. Deciden los bytes.
     */
    @Test
    void subirJpegDeclaradoComoOctetStream_seAceptaPorqueDecidenLosBytes() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        mockMvc.perform(subida(ficha, "sin-tipo", "application/octet-stream",
                        ImagenesDePrueba.jpeg()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoContenido").value("image/jpeg"));
    }

    @Test
    void subirArchivoVacio_devuelve400() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        mockMvc.perform(subida(ficha, "vacia.jpg", "image/jpeg", new byte[0]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(Matchers.containsString("vacío")));

        assertNadaEscrito();
    }

    /**
     * El limite de 20MB. En produccion lo aplica el parser de multipart antes
     * de llegar al controlador; aqui lo aplica la comprobacion de
     * {@code AlmacenamientoDeImagenes}, que lee <strong>la misma propiedad</strong>
     * ({@code spring.servlet.multipart.max-file-size}) para que no haya dos
     * numeros que puedan discrepar. Las dos vias dan 413.
     * <p>
     * Hace falta que la comprobacion exista en Java justamente por esto: MockMvc
     * no pasa por el parser real, asi que sin ella este caso no se podria
     * probar sin levantar el servidor.
     */
    @Test
    void subirMasDe20MB_devuelve413YNoEscribeNada() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        mockMvc.perform(subida(ficha, "enorme.jpg", "image/jpeg",
                        ImagenesDePrueba.jpegDeMegas(21)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.estado").value(413))
                .andExpect(jsonPath("$.error").value("Archivo demasiado grande"))
                .andExpect(jsonPath("$.mensaje").value(Matchers.containsString("20 MB")));

        assertNadaEscrito();
        assertSinImagenes(ficha);
    }

    @Test
    void subirSinTextoAlternativo_devuelve400() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        mockMvc.perform(multipart(RUTA_SUBIDA.formatted(ficha.id()))
                        .file(parte(ImagenesDePrueba.jpeg(), "planta.jpg", "image/jpeg"))
                        .param("tipoImagen", "HOJA")
                        .header(HttpHeaders.AUTHORIZATION, token(MULTIMEDIA)))
                .andExpect(status().isBadRequest());

        assertNadaEscrito();
    }

    @Test
    void subirSinLaParteDelArchivo_devuelve400() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        mockMvc.perform(multipart(RUTA_SUBIDA.formatted(ficha.id()))
                        .param("tipoImagen", "HOJA")
                        .param("textoAlternativo", "Sin archivo")
                        .header(HttpHeaders.AUTHORIZATION, token(MULTIMEDIA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Parte faltante"));

        assertNadaEscrito();
    }

    @Test
    void subirAUnaEspecieQueNoExiste_devuelve404YNoEscribeNada() throws Exception {
        mockMvc.perform(multipart(RUTA_SUBIDA.formatted(999_999_999L))
                        .file(parte(ImagenesDePrueba.jpeg(), "planta.jpg", "image/jpeg"))
                        .param("tipoImagen", "HOJA")
                        .param("textoAlternativo", "A ninguna parte")
                        .header(HttpHeaders.AUTHORIZATION, token(MULTIMEDIA)))
                .andExpect(status().isNotFound());

        assertNadaEscrito();
    }

    // ---------- Permisos ----------

    @Test
    void subirAnonimo_devuelve401YNoEscribeNada() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        mockMvc.perform(multipart(RUTA_SUBIDA.formatted(ficha.id()))
                        .file(parte(ImagenesDePrueba.jpeg(), "planta.jpg", "image/jpeg"))
                        .param("tipoImagen", "HOJA")
                        .param("textoAlternativo", "Anónima"))
                .andExpect(status().isUnauthorized());

        assertNadaEscrito();
        assertSinImagenes(ficha);
    }

    /** Mismo permiso que registrar un enlace: sin el, 403. */
    @Test
    void subirSinMultimediaGestionar_devuelve403YNoEscribeNada() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        mockMvc.perform(multipart(RUTA_SUBIDA.formatted(ficha.id()))
                        .file(parte(ImagenesDePrueba.jpeg(), "planta.jpg", "image/jpeg"))
                        .param("tipoImagen", "HOJA")
                        .param("textoAlternativo", "Sin permiso")
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_CONSULTAR)))
                .andExpect(status().isForbidden());

        assertNadaEscrito();
        assertSinImagenes(ficha);
    }

    // ---------- El enlace externo sigue funcionando igual ----------

    @Test
    void registrarPorUrl_sigueCreandoUnaImagenDeOrigenUrl() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        String cuerpo = mockMvc.perform(post("/api/especies/" + ficha.id() + "/imagenes")
                        .header(HttpHeaders.AUTHORIZATION, token(MULTIMEDIA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoImagen": "FLOR",
                                  "urlImagen": "https://example.invalid/enlace-%d.jpg",
                                  "textoAlternativo": "Enlace externo de prueba"
                                }
                                """.formatted(SECUENCIA.incrementAndGet())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origen").value("URL"))
                .andExpect(jsonPath("$.urlImagen").exists())
                // Los campos de archivo no aplican a un enlace.
                .andExpect(jsonPath("$.rutaArchivo").doesNotExist())
                .andExpect(jsonPath("$.tipoContenido").doesNotExist())
                .andExpect(jsonPath("$.tamanioBytes").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        ImagenEspecie guardada = recargar(idDe(cuerpo));
        assertEquals(OrigenImagen.URL, guardada.getOrigen());
        assertNull(guardada.getRutaArchivo(), "un enlace quedo con nombre de archivo");

        // Y no se escribio nada en la carpeta: un enlace no tiene binario.
        assertNadaEscrito();
    }

    /** Un enlace no tiene archivo que servir, y decirlo es un 409, no un 404. */
    @Test
    void servirElArchivoDeUnEnlace_devuelve409() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);
        Long idImagen = ficha.imagenEspecie().getIdImagen();

        mockMvc.perform(get("/api/imagenes-especie/" + idImagen + "/archivo"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje")
                        .value(Matchers.containsString("enlace externo")));
    }

    // ---------- Subir no mueve el flujo editorial ----------

    /**
     * Una imagen es un sub-recurso: anadirla no manda la ficha a revision. Lo
     * unico que cambia en la especie es su fecha, y eso lo prueba
     * {@code ImagenRefrescaFechaDeEspecieTest}, que es donde se puede medir.
     */
    @Test
    void subirAUnaFichaPublicada_laDejaPublicadaYEnElCatalogo() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        mockMvc.perform(subida(ficha, "planta.jpg", "image/jpeg", ImagenesDePrueba.jpeg()))
                .andExpect(status().isCreated());

        entityManager.flush();

        mockMvc.perform(get("/api/especies/publicadas/" + ficha.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPublicacion").value("PUBLICADA"))
                .andExpect(jsonPath("$.validadaPor").exists())
                .andExpect(jsonPath("$.publicadaPor").exists());

        mockMvc.perform(get("/api/especies/publicadas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].idEspecie")
                        .value(Matchers.hasItem(ficha.id().intValue())));
    }

    // ---------- Baja logica: la fila se marca, el archivo se queda ----------

    @Test
    void bajaLogicaDeUnaPropia_dejaLaFilaInactivaYElArchivoEnDisco() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        Long idImagen = idDe(mockMvc.perform(
                        subida(ficha, "planta.jpg", "image/jpeg", ImagenesDePrueba.jpeg()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        String nombreArchivo = recargar(idImagen).getRutaArchivo();

        mockMvc.perform(delete("/api/imagenes-especie/" + idImagen)
                        .header(HttpHeaders.AUTHORIZATION, token(MULTIMEDIA)))
                .andExpect(status().isNoContent());

        ImagenEspecie dadaDeBaja = recargar(idImagen);
        assertFalse(dadaDeBaja.getActiva(), "la fila no quedo inactiva");
        assertFalse(dadaDeBaja.getEsPrincipal(), "una imagen inactiva quedo como principal");
        assertEquals(nombreArchivo, dadaDeBaja.getRutaArchivo(),
                "la baja borro la referencia al archivo");
        assertTrue(CarpetaDeImagenesDePrueba.existe(nombreArchivo),
                "la baja logica borro el archivo del disco; tiene que ser reversible");

        // Y reactivarla la vuelve a servir, sin tener que subirla otra vez.
        mockMvc.perform(patch("/api/imagenes-especie/" + idImagen + "/activar")
                        .header(HttpHeaders.AUTHORIZATION, token(MULTIMEDIA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(true));

        mockMvc.perform(get("/api/imagenes-especie/" + idImagen + "/archivo"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(ImagenesDePrueba.jpeg()));
    }

    /**
     * El PUT reemplaza la URL, que una imagen propia no tiene. Se corta con un
     * 409 que lo explica, en vez de dejar que
     * {@code ck_imagen_fuente_unica} devuelva un 409 de integridad sin mensaje
     * util.
     */
    @Test
    void editarUnaPropiaPorElPutDeUrl_devuelve409ConMensajeClaro() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        Long idImagen = idDe(mockMvc.perform(
                        subida(ficha, "planta.jpg", "image/jpeg", ImagenesDePrueba.jpeg()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(put("/api/imagenes-especie/" + idImagen)
                        .header(HttpHeaders.AUTHORIZATION, token(MULTIMEDIA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoImagen": "HOJA",
                                  "urlImagen": "https://example.invalid/sustituta.jpg",
                                  "textoAlternativo": "Intento de reemplazo"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje")
                        .value(Matchers.containsString("archivo propio")));

        ImagenEspecie sinTocar = recargar(idImagen);
        assertEquals(OrigenImagen.PROPIA, sinTocar.getOrigen(), "el 409 cambio el origen");
        assertNull(sinTocar.getUrlImagen(), "el 409 escribio la URL igualmente");
    }

    // ---------- Listado ----------

    @Test
    void elListadoDeLaEspecie_distingueLasPropiasDeLosEnlaces() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        mockMvc.perform(subida(ficha, "planta.jpg", "image/jpeg", ImagenesDePrueba.jpeg()))
                .andExpect(status().isCreated());

        entityManager.flush();

        mockMvc.perform(get("/api/especies/" + ficha.id() + "/imagenes"))
                .andExpect(status().isOk())
                // La que siembra la ficha es un enlace; la que se acaba de
                // subir es propia.
                .andExpect(jsonPath("$[*].origen")
                        .value(Matchers.hasItems("URL", "PROPIA")));
    }

    // ---------- Apoyo ----------

    private Ficha sembrar(EstadoPublicacion estado) {
        return new SiembraDeFichas(entityManager).sembrar(estado);
    }

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
            subidaBase(Ficha ficha) {
        return multipart(RUTA_SUBIDA.formatted(ficha.id()));
    }

    /** La subida completa y valida, cambiando solo el archivo. */
    private org.springframework.test.web.servlet.RequestBuilder subida(Ficha ficha,
            String nombre, String tipoDeclarado, byte[] contenido) {

        return subidaBase(ficha)
                .file(parte(contenido, nombre, tipoDeclarado))
                .param("tipoImagen", "HOJA")
                .param("textoAlternativo", "Imagen de prueba subida como archivo")
                .param("titulo", "Prueba de subida")
                .header(HttpHeaders.AUTHORIZATION, token(MULTIMEDIA));
    }

    private static MockMultipartFile parte(byte[] contenido, String nombre, String tipo) {
        return new MockMultipartFile("archivo", nombre, tipo, contenido);
    }

    private String token(String... permisos) {
        UsuarioResponse usuario = UsuarioResponse.builder()
                .id(idUsuario)
                .nombres("Subida")
                .apellidos("De Imagenes")
                .correo("subida.imagenes@prueba.local")
                .nombreUsuario("subida.imagenes")
                .activo(true)
                .build();

        return "Bearer "
                + jwtService.emitir(usuario, List.of("CONSULTOR"), List.of(permisos)).token();
    }

    /** El id de la imagen que devolvio el endpoint. */
    private static Long idDe(String cuerpoJson) {
        java.util.regex.Matcher coincidencia =
                java.util.regex.Pattern.compile("\"idImagen\":(\\d+)").matcher(cuerpoJson);
        assertTrue(coincidencia.find(), "la respuesta no trae idImagen: " + cuerpoJson);
        return Long.valueOf(coincidencia.group(1));
    }

    private ImagenEspecie recargar(Long idImagen) {
        entityManager.flush();
        return imagenEspecieRepository.findById(idImagen).orElseThrow();
    }

    /**
     * La carpeta sigue vacia. Es la mitad de cada prueba de rechazo: un 400 que
     * ya hubiera escrito el archivo no habria validado "antes de escribir".
     */
    private static void assertNadaEscrito() {
        assertEquals(0L, CarpetaDeImagenesDePrueba.archivos(),
                "se escribio un archivo en la carpeta: " + CarpetaDeImagenesDePrueba.carpeta());
    }

    /** La especie no gano ninguna imagen propia. */
    private void assertSinImagenes(Ficha ficha) {
        entityManager.flush();
        List<ImagenEspecie> propias = imagenEspecieRepository
                .findByEspecieIdEspecieOrderByIdImagenAsc(ficha.id())
                .stream()
                .filter(imagen -> imagen.getOrigen() == OrigenImagen.PROPIA)
                .toList();

        assertArrayEquals(new Object[0], propias.toArray(),
                "quedo una fila de imagen propia para un archivo que se rechazo");
    }
}
