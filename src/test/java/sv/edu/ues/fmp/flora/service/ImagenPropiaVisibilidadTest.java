package sv.edu.ues.fmp.flora.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
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
import sv.edu.ues.fmp.flora.entity.enums.EstadoPublicacion;
import sv.edu.ues.fmp.flora.repository.UsuarioRepository;

/**
 * Quien puede descargar el archivo de una imagen propia.
 *
 * <h2>Por que esto es la mitad del cambio</h2>
 * Guardar el binario en una carpeta del servidor crea una forma de leerlo que
 * antes no existia. La tentacion es mapear la carpeta como recurso estatico y
 * dar la URL: eso serviria los archivos sin pasar por
 * {@code VisibilidadDeEspecie}, de modo que las imagenes de las fichas en
 * BORRADOR y EN_REVISION quedarian publicas para quien supiera un nombre.
 * Exactamente el agujero que ya se cerro con los sub-recursos de especie. Por
 * eso el archivo lo sirve un endpoint del controlador y esta clase comprueba
 * que hereda la regla de la ficha.
 *
 * <h2>404 y no 403</h2>
 * Un 403 sobre la imagen de un borrador confirmaria dos cosas: que ese id de
 * imagen existe y que pertenece a una ficha sin publicar. Con eso se puede ir
 * probando numeros y averiguar que hay en preparacion. El 404 --con el mismo
 * texto que un id inventado-- no distingue nada, y
 * {@link #elArchivoDeUnBorradorYUnIdInventado_respondenExactamenteLoMismo()}
 * compara los dos cuerpos para que no se separen con el tiempo.
 *
 * <h2>Residuo</h2>
 * {@code @Transactional} para la base y el vaciado de la carpeta temporal para
 * los archivos, que la transaccion no revierte.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImagenPropiaVisibilidadTest {

    private static final String MULTIMEDIA = "MULTIMEDIA_GESTIONAR";
    private static final String ESPECIE_CONSULTAR = "ESPECIE_CONSULTAR";

    /** No existe: es la referencia con la que se compara el 404 del borrador. */
    private static final long ID_INVENTADO = 999_999_999L;

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
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private EmailService emailService;

    private Long idUsuario;

    @BeforeEach
    void preparar() {
        idUsuario = UsuariosDePrueba.idActivo(usuarioRepository);
        CarpetaDeImagenesDePrueba.limpiar();
    }

    @AfterEach
    void limpiarCarpeta() {
        CarpetaDeImagenesDePrueba.limpiar();
    }

    // ---------- Ficha publicada: la ve cualquiera ----------

    @Test
    void elArchivoDeUnaFichaPublicada_seSirveSinToken() throws Exception {
        Long idImagen = subirA(EstadoPublicacion.PUBLICADA);

        mockMvc.perform(get(ruta(idImagen)))
                .andExpect(status().isOk())
                .andExpect(content().bytes(ImagenesDePrueba.jpeg()));
    }

    // ---------- Fichas sin publicar: 404 salvo con ESPECIE_CONSULTAR ----------

    @Test
    void elArchivoDeUnBorradorAnonimo_devuelve404() throws Exception {
        Long idImagen = subirA(EstadoPublicacion.BORRADOR);

        mockMvc.perform(get(ruta(idImagen)))
                .andExpect(status().isNotFound());
    }

    /**
     * Con token valido pero sin {@code ESPECIE_CONSULTAR} tampoco: la
     * respuesta no puede depender de traer un token cualquiera, porque entonces
     * cualquier cuenta serviria de oraculo.
     */
    @Test
    void elArchivoDeUnBorradorSinEsPecieConsultar_devuelve404() throws Exception {
        Long idImagen = subirA(EstadoPublicacion.BORRADOR);

        mockMvc.perform(get(ruta(idImagen))
                        .header(HttpHeaders.AUTHORIZATION, token(MULTIMEDIA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void elArchivoDeUnaEnRevisionAnonimo_devuelve404() throws Exception {
        Long idImagen = subirA(EstadoPublicacion.EN_REVISION);

        mockMvc.perform(get(ruta(idImagen)))
                .andExpect(status().isNotFound());
    }

    @Test
    void elArchivoDeUnaRechazadaAnonimo_devuelve404() throws Exception {
        Long idImagen = subirA(EstadoPublicacion.RECHAZADA);

        mockMvc.perform(get(ruta(idImagen)))
                .andExpect(status().isNotFound());
    }

    /** Con el permiso de consulta interna si se ve: el 404 es por visibilidad, no un bloqueo. */
    @Test
    void elArchivoDeUnBorradorConEsPecieConsultar_seSirve() throws Exception {
        Long idImagen = subirA(EstadoPublicacion.BORRADOR);

        mockMvc.perform(get(ruta(idImagen))
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_CONSULTAR)))
                .andExpect(status().isOk())
                .andExpect(content().bytes(ImagenesDePrueba.jpeg()));
    }

    // ---------- Indistinguible ----------

    /**
     * Los dos cuerpos tienen que ser identicos salvo la ruta, que es distinta
     * por el id. Si algun dia uno dijera "no tiene permiso" y el otro "no
     * existe", el borrador se podria detectar comparando respuestas.
     */
    @Test
    void elArchivoDeUnBorradorYUnIdInventado_respondenExactamenteLoMismo() throws Exception {
        Long idImagen = subirA(EstadoPublicacion.BORRADOR);

        String delBorrador = mockMvc.perform(get(ruta(idImagen)))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        String delInventado = mockMvc.perform(get(ruta(ID_INVENTADO)))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertEquals(mensajeDe(delInventado.replace(String.valueOf(ID_INVENTADO), "X")),
                mensajeDe(delBorrador.replace(String.valueOf(idImagen), "X")),
                "el 404 del borrador no es indistinguible del de un id inexistente");
    }

    @Test
    void elListadoDeImagenesDeUnBorradorAnonimo_sigueDevolviendo404() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.BORRADOR);

        // Regresion: el guard del listado ya existia y el cambio no lo toca.
        mockMvc.perform(get("/api/especies/" + ficha.id() + "/imagenes"))
                .andExpect(status().isNotFound());
    }

    // ---------- Apoyo ----------

    private static String ruta(Long idImagen) {
        return "/api/imagenes-especie/" + idImagen + "/archivo";
    }

    /** Siembra una ficha en el estado dado y le sube una imagen propia. */
    private Long subirA(EstadoPublicacion estado) throws Exception {
        Ficha ficha = sembrar(estado);

        String cuerpo = mockMvc.perform(multipart("/api/especies/" + ficha.id()
                        + "/imagenes/archivo")
                        .file(new MockMultipartFile("archivo", "planta.jpg", "image/jpeg",
                                ImagenesDePrueba.jpeg()))
                        .param("tipoImagen", "HOJA")
                        .param("textoAlternativo", "Imagen de prueba de visibilidad")
                        .header(HttpHeaders.AUTHORIZATION, token(MULTIMEDIA)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        entityManager.flush();

        java.util.regex.Matcher coincidencia =
                java.util.regex.Pattern.compile("\"idImagen\":(\\d+)").matcher(cuerpo);
        assertTrue(coincidencia.find(), "la respuesta no trae idImagen: " + cuerpo);
        return Long.valueOf(coincidencia.group(1));
    }

    private Ficha sembrar(EstadoPublicacion estado) {
        return new SiembraDeFichas(entityManager).sembrar(estado);
    }

    /** El campo mensaje del cuerpo de error, que es lo que podria delatar. */
    private static String mensajeDe(String cuerpoJson) {
        java.util.regex.Matcher coincidencia =
                java.util.regex.Pattern.compile("\"mensaje\":\"([^\"]*)\"").matcher(cuerpoJson);
        return coincidencia.find() ? coincidencia.group(1) : cuerpoJson;
    }

    private String token(String... permisos) {
        UsuarioResponse usuario = UsuarioResponse.builder()
                .id(idUsuario)
                .nombres("Visibilidad")
                .apellidos("De Archivos")
                .correo("visibilidad.archivos@prueba.local")
                .nombreUsuario("visibilidad.archivos")
                .activo(true)
                .build();

        return "Bearer "
                + jwtService.emitir(usuario, List.of("CONSULTOR"), List.of(permisos)).token();
    }
}
