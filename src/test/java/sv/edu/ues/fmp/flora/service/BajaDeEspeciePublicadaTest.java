package sv.edu.ues.fmp.flora.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
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
 * Una especie PUBLICADA no se puede dar de baja: revision #9 del Dr. Dagoberto.
 *
 * <h2>La regla</h2>
 * {@code DELETE /api/especies/{id}} es una baja logica
 * ({@code activa = false}), y sobre una ficha publicada hacia algo que no deja
 * rastro donde deberia: la sacaba del catalogo publico --{@code listarPublicadas()}
 * filtra por {@code activa}-- mientras su {@code estado_publicacion} seguia
 * diciendo PUBLICADA. La unica senal de que la ficha se habia retirado era una
 * bandera que el publico no ve. Para retirar algo del catalogo existe la via
 * editorial, que si deja rastro: editarla la devuelve a EN_REVISION, y desde
 * ahi se la puede rechazar y dar de baja.
 * <p>
 * Los otros tres estados no cambian: BORRADOR, EN_REVISION y RECHAZADA no estan
 * en el catalogo publico, asi que no hay nada que retirar ni rastro que perder,
 * y se siguen dando de baja con 204.
 *
 * <h2>Lo que cada prueba fija</h2>
 * El 409 no basta por si solo: lo que importa es que la ficha siga <em>intacta
 * y visible</em>. Por eso
 * {@link #baja_deUnaPublicada_devuelve409YSigueEnElCatalogo()} comprueba las
 * tres cosas --el 409, que {@code activa} sigue en true y que la ficha sigue
 * saliendo en {@code /api/especies/publicadas}--: con solo el codigo, un guard
 * que lanzara despues de escribir pasaria la prueba y habria dejado la ficha
 * fuera del catalogo.
 *
 * <h2>La autorizacion precede al guard</h2>
 * El guard vive en el servicio y el {@code @PreAuthorize('ESPECIE_EDITAR')} en
 * el controlador, asi que una peticion sin token o sin ese permiso no llega a
 * tocar la regla: sale 401 o 403, no 409. Lo fijan
 * {@link #baja_deUnaPublicadaAnonima_devuelve401()} y
 * {@link #baja_deUnaPublicadaSinPermiso_devuelve403()}, que ademas comprueban
 * que la ficha queda igual --el motivo del rechazo cambia, el efecto sobre el
 * catalogo no--.
 *
 * <h2>Residuo</h2>
 * {@code @Transactional}: el usuario y las fichas sembradas viven en la
 * transaccion de la prueba y el rollback se los lleva. No hay commit en
 * ninguna parte.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BajaDeEspeciePublicadaTest {

    private static final String ESPECIE_EDITAR = "ESPECIE_EDITAR";
    private static final String ESPECIE_CONSULTAR = "ESPECIE_CONSULTAR";

    private static final String LISTADO_PUBLICO = "/api/especies/publicadas";

    private static final String MENSAJE = "No se puede dar de baja una especie publicada";

    /** Para que los correos y nombres de usuario no choquen entre si. */
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
                .nombres("Baja")
                .apellidos("De Publicada")
                .correo("baja.publicada." + n + "@prueba.local")
                .nombreUsuario("baja.publicada." + n)
                // No es una clave usable: este usuario no inicia sesion, el
                // token se emite directamente con JwtService.
                .claveHash("$2a$10$pruebaDeBajaSinClaveUtilizable0000000000000000000000000")
                .activo(true)
                .build();

        entityManager.persist(editor);
        entityManager.flush();
    }

    // ---------- La regla ----------

    @Test
    void baja_deUnaPublicada_devuelve409YSigueEnElCatalogo() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        mockMvc.perform(delete("/api/especies/" + ficha.id())
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.error").value("Transición de estado no válida"))
                .andExpect(jsonPath("$.mensaje").value(MENSAJE));

        Especie sinTocar = recargar(ficha.id());
        assertTrue(sinTocar.getActiva(), "el 409 dio de baja la ficha igualmente");
        assertEquals(EstadoPublicacion.PUBLICADA, sinTocar.getEstadoPublicacion(),
                "el 409 movio el estado de publicacion");

        // Y lo que de verdad importa: la ficha no desaparecio del catalogo
        // publico. El guard que lanzara despues de escribir pasaria las
        // comprobaciones de arriba y fallaria esta.
        mockMvc.perform(get(LISTADO_PUBLICO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].idEspecie")
                        .value(Matchers.hasItem(ficha.id().intValue())));
    }

    // ---------- Los otros tres estados se siguen dando de baja ----------

    @Test
    void baja_deUnBorrador_devuelve204YLaDesactiva() throws Exception {
        bajaPermitida(EstadoPublicacion.BORRADOR);
    }

    @Test
    void baja_deUnaEnRevision_devuelve204YLaDesactiva() throws Exception {
        bajaPermitida(EstadoPublicacion.EN_REVISION);
    }

    @Test
    void baja_deUnaRechazada_devuelve204YLaDesactiva() throws Exception {
        bajaPermitida(EstadoPublicacion.RECHAZADA);
    }

    // ---------- La autorizacion precede al guard ----------

    @Test
    void baja_deUnaPublicadaAnonima_devuelve401() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        mockMvc.perform(delete("/api/especies/" + ficha.id()))
                .andExpect(status().isUnauthorized());

        assertTrue(recargar(ficha.id()).getActiva(),
                "una peticion anonima dio de baja la ficha");
    }

    /**
     * Con token valido pero sin {@code ESPECIE_EDITAR}: el
     * {@code @PreAuthorize} del controlador corta antes de que el servicio
     * pueda mirar el estado, asi que es un 403 y no el 409 de la regla.
     */
    @Test
    void baja_deUnaPublicadaSinPermiso_devuelve403() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.PUBLICADA);

        mockMvc.perform(delete("/api/especies/" + ficha.id())
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_CONSULTAR)))
                .andExpect(status().isForbidden());

        assertTrue(recargar(ficha.id()).getActiva(),
                "una peticion sin ESPECIE_EDITAR dio de baja la ficha");
    }

    /**
     * Y el mismo 403 sobre una ficha que si se podria dar de baja: asi queda
     * claro que el 403 viene del permiso y no del estado.
     */
    @Test
    void baja_deUnBorradorSinPermiso_devuelve403() throws Exception {
        Ficha ficha = sembrar(EstadoPublicacion.BORRADOR);

        mockMvc.perform(delete("/api/especies/" + ficha.id())
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_CONSULTAR)))
                .andExpect(status().isForbidden());

        assertTrue(recargar(ficha.id()).getActiva(),
                "una peticion sin ESPECIE_EDITAR dio de baja el borrador");
    }

    // ---------- Apoyo ----------

    private void bajaPermitida(EstadoPublicacion estado) throws Exception {
        Ficha ficha = sembrar(estado);

        mockMvc.perform(delete("/api/especies/" + ficha.id())
                        .header(HttpHeaders.AUTHORIZATION, token(ESPECIE_EDITAR)))
                .andExpect(status().isNoContent());

        Especie guardada = recargar(ficha.id());
        assertFalse(guardada.getActiva(), "no quedo dada de baja estando en " + estado);
        // La baja es logica y no toca el flujo editorial: el estado se conserva
        // para que activar() devuelva la ficha al punto en que estaba.
        assertEquals(estado, guardada.getEstadoPublicacion(), "la baja movio el estado");
    }

    private Ficha sembrar(EstadoPublicacion estado) {
        return new SiembraDeFichas(entityManager).sembrar(estado);
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

    private Especie recargar(Long id) {
        entityManager.flush();
        return especieRepository.findById(id).orElseThrow();
    }
}
