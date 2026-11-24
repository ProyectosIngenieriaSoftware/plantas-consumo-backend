package sv.edu.ues.fmp.flora.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import sv.edu.ues.fmp.flora.config.SiembraDeFichas;
import sv.edu.ues.fmp.flora.config.SiembraDeFichas.Ficha;
import sv.edu.ues.fmp.flora.config.UsuariosDePrueba;
import sv.edu.ues.fmp.flora.dto.response.UsuarioResponse;
import sv.edu.ues.fmp.flora.entity.Beneficio;
import sv.edu.ues.fmp.flora.entity.ParteBeneficio;
import sv.edu.ues.fmp.flora.entity.enums.EstadoPublicacion;
import sv.edu.ues.fmp.flora.entity.enums.ParteBeneficioId;
import sv.edu.ues.fmp.flora.entity.enums.TipoBeneficio;
import sv.edu.ues.fmp.flora.repository.UsuarioRepository;

/**
 * La baja logica de {@code beneficio}: que el DELETE retire el beneficio en
 * lugar de borrarlo, y que retirarlo lo saque del catalogo publico.
 *
 * <h2>Que cambio y por que</h2>
 * {@code DELETE /api/beneficios/{id}} borraba la fila. Eso tenia dos defectos.
 * El primero: un beneficio ya citado por alguna ficha no se podia retirar,
 * porque la FK de {@code parte_beneficio} saltaba y el
 * {@code GlobalExceptionHandler} la traducia a un <strong>409</strong> cuyo
 * mensaje --"La operacion viola una restriccion de la base de datos"-- no le
 * decia a nadie que el problema era que el beneficio estaba en uso. El
 * segundo: cuando si se podia borrar, la fila desaparecia del historial. El
 * resto del catalogo no funciona asi: {@code habitat} y {@code parte_planta}
 * dan de baja con el DELETE y recuperan con {@code PATCH /{id}/activar}. Esta
 * clase fija ese contrato para {@code beneficio}.
 *
 * <h2>El caso que cruza los dos modulos</h2>
 * El que no es evidente es {@link #beneficioRetirado_desapareceDelCatalogoPublico()}.
 * Dar de baja el beneficio <strong>no</strong> toca sus filas de
 * {@code parte_beneficio}: siguen con {@code activa = true}, porque la baja es
 * del catalogo y no de cada asociacion. Si el listado por parte solo mirara su
 * propia bandera --como hacia--, un beneficio retirado seguiria pintandose en
 * la ficha publica. De ahi la segunda condicion de la consulta de
 * {@code ParteBeneficioRepository}.
 *
 * <h2>Cero residuo</h2>
 * {@code @SpringBootTest @Transactional}: la ficha que siembra
 * {@link SiembraDeFichas}, los beneficios que siembra esta clase y lo que
 * escriben los DELETE y los PATCH viven en la transaccion de la prueba, y el
 * rollback se lo lleva. No hay commit en ninguna parte. El prefijo
 * {@code Zzbeneficio} evita choques de nombre con el catalogo real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BeneficioBajaLogicaTest {

    private static final String CATALOGO_GESTIONAR = "CATALOGO_GESTIONAR";

    /** Un permiso real del proyecto, pero de otro modulo: sirve para el 403. */
    private static final String OTRO_PERMISO = "MULTIMEDIA_GESTIONAR";

    private static final String BENEFICIOS = "/api/beneficios";

    private static final String POR_PARTE = "/api/parte-beneficios/parte/";

    private static final long ID_INEXISTENTE = 999999L;

    /** Distingue los nombres que siembra esta clase de los de cualquier otra. */
    private static final AtomicLong SECUENCIA = new AtomicLong();

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private EntityManager entityManager;

    @MockitoBean private EmailService emailService;

    private Ficha publicada;

    @BeforeEach
    void sembrar() {
        publicada = new SiembraDeFichas(entityManager).sembrar(EstadoPublicacion.PUBLICADA);
    }

    // ---------- 1. El DELETE retira, no borra ----------

    /**
     * El cambio de contrato, en su forma mas simple: tras el DELETE el
     * beneficio sigue consultandose --200, no 404-- y lo que cambio es su
     * bandera.
     */
    @Test
    void delete_dejaElBeneficioConActivoFalse_yNoEnUn404() throws Exception {
        Long id = sembrarBeneficio(true).getIdBeneficio();

        mockMvc.perform(delete(BENEFICIOS + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, tokenCon(CATALOGO_GESTIONAR)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BENEFICIOS + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idBeneficio").value(id))
                .andExpect(jsonPath("$.activo").value(false));
    }

    /**
     * El caso que antes era imposible. El beneficio tiene una asociacion viva
     * en {@code parte_beneficio}, asi que el borrado fisico moria en la FK y
     * salia un 409. Ahora responde 204, y la asociacion sigue donde estaba:
     * la ficha no pierde su historia, solo deja de pintar ese beneficio.
     */
    @Test
    void delete_deUnBeneficioUsadoPorUnaFicha_responde204YNoUn409() throws Exception {
        Beneficio beneficio = sembrarBeneficio(true);
        ParteBeneficio asociacion = asociarA(publicada, beneficio);
        Long id = beneficio.getIdBeneficio();

        mockMvc.perform(delete(BENEFICIOS + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, tokenCon(CATALOGO_GESTIONAR)))
                .andExpect(status().isNoContent());

        // La fila de parte_beneficio sobrevive y nadie le toco su bandera.
        mockMvc.perform(get("/api/parte-beneficios/"
                        + asociacion.getId().getIdEspecieParte() + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(true));
    }

    /** El 404 de un id que no existe no cambia. */
    @Test
    void delete_deUnIdInexistente_sigueSiendo404() throws Exception {
        mockMvc.perform(delete(BENEFICIOS + "/" + ID_INEXISTENTE)
                        .header(HttpHeaders.AUTHORIZATION, tokenCon(CATALOGO_GESTIONAR)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404));
    }

    // ---------- 2. El PATCH devuelve lo retirado ----------

    @Test
    void patchActivar_devuelveElBeneficioAlCatalogo() throws Exception {
        Long id = sembrarBeneficio(false).getIdBeneficio();

        mockMvc.perform(patch(BENEFICIOS + "/" + id + "/activar")
                        .header(HttpHeaders.AUTHORIZATION, tokenCon(CATALOGO_GESTIONAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idBeneficio").value(id))
                .andExpect(jsonPath("$.activo").value(true));

        mockMvc.perform(get(BENEFICIOS + "?soloActivos=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idBeneficio == " + id + ")]").isNotEmpty());
    }

    /** Idempotente, como sus hermanos del catalogo: 200 y nada que cambiar. */
    @Test
    void patchActivar_sobreUnoYaActivo_sigueSiendo200() throws Exception {
        Long id = sembrarBeneficio(true).getIdBeneficio();

        mockMvc.perform(patch(BENEFICIOS + "/" + id + "/activar")
                        .header(HttpHeaders.AUTHORIZATION, tokenCon(CATALOGO_GESTIONAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
    }

    // ---------- 3. Retirado no se lista, ni aqui ni en la ficha ----------

    /**
     * El caso cruzado. Antes del DELETE el beneficio sale en el listado de
     * activos y en los beneficios de la parte; despues, en ninguno de los dos
     * --aunque su asociacion siga activa-- y sigue saliendo en el listado
     * completo, que es lo que permite reactivarlo.
     */
    @Test
    void beneficioRetirado_desapareceDelCatalogoPublico() throws Exception {
        Beneficio beneficio = sembrarBeneficio(true);
        ParteBeneficio asociacion = asociarA(publicada, beneficio);
        Long id = beneficio.getIdBeneficio();
        String rutaDeLaParte = POR_PARTE + asociacion.getId().getIdEspecieParte();

        // Linea base: con el beneficio activo se ve por las dos puertas.
        mockMvc.perform(get(BENEFICIOS + "?soloActivos=true"))
                .andExpect(jsonPath("$[?(@.idBeneficio == " + id + ")]").isNotEmpty());
        mockMvc.perform(get(rutaDeLaParte))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idBeneficio == " + id + ")]").isNotEmpty());

        mockMvc.perform(delete(BENEFICIOS + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, tokenCon(CATALOGO_GESTIONAR)))
                .andExpect(status().isNoContent());

        // Fuera del listado de activos...
        mockMvc.perform(get(BENEFICIOS + "?soloActivos=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idBeneficio == " + id + ")]").isEmpty());

        // ...y fuera de la ficha publica, que es lo que el filtro nuevo tapa.
        mockMvc.perform(get(rutaDeLaParte))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idBeneficio == " + id + ")]").isEmpty());

        // Pero el listado completo lo sigue viendo, con su bandera en false.
        mockMvc.perform(get(BENEFICIOS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idBeneficio == " + id
                        + " && @.activo == false)]").isNotEmpty());
    }

    /**
     * Contrapeso: el beneficio activo se lista con normalidad por las tres
     * puertas. Sin este caso, un filtro roto que no devolviera nada tambien
     * pasaria la prueba de arriba.
     */
    @Test
    void beneficioActivo_seListaConNormalidad() throws Exception {
        Beneficio beneficio = sembrarBeneficio(true);
        ParteBeneficio asociacion = asociarA(publicada, beneficio);
        Long id = beneficio.getIdBeneficio();

        mockMvc.perform(get(BENEFICIOS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idBeneficio == " + id + ")]").isNotEmpty());

        mockMvc.perform(get(BENEFICIOS + "?soloActivos=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idBeneficio == " + id + ")]").isNotEmpty());

        mockMvc.perform(get(POR_PARTE + asociacion.getId().getIdEspecieParte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idBeneficio == " + id
                        + " && @.activa == true)]").isNotEmpty());
    }

    // ---------- 4. El PATCH pide lo mismo que el DELETE ----------

    /**
     * {@code CATALOGO_GESTIONAR} en las dos rutas: quien puede retirar un
     * beneficio es quien puede devolverlo. El 401 del anonimo lo pone la
     * matriz de seguridad --{@code CATALOGO_PUBLICO} abre {@code /api/beneficios/**}
     * solo para GET-- y el 403 lo pone el {@code @PreAuthorize}.
     */
    @Test
    void patchActivar_exigeElMismoPermisoQueElDelete() throws Exception {
        Long id = sembrarBeneficio(true).getIdBeneficio();

        for (String ruta : List.of(
                BENEFICIOS + "/" + id + "/activar",
                BENEFICIOS + "/" + id)) {

            boolean esElPatch = ruta.endsWith("/activar");

            mockMvc.perform(esElPatch ? patch(ruta) : delete(ruta))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.estado").value(401));

            mockMvc.perform((esElPatch ? patch(ruta) : delete(ruta))
                            .header(HttpHeaders.AUTHORIZATION, tokenCon(OTRO_PERMISO)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.estado").value(403));

            mockMvc.perform((esElPatch ? patch(ruta) : delete(ruta))
                            .header(HttpHeaders.AUTHORIZATION, tokenCon(CATALOGO_GESTIONAR)))
                    .andExpect(esElPatch ? status().isOk() : status().isNoContent());
        }
    }

    // ---------- Siembra propia de esta clase ----------

    private Beneficio sembrarBeneficio(boolean activo) {
        Beneficio beneficio = Beneficio.builder()
                .nombre("Zzbeneficio baja logica " + SECUENCIA.incrementAndGet())
                .tipoBeneficio(TipoBeneficio.OTRO)
                .descripcion("Beneficio sembrado por las pruebas de baja lógica.")
                .activo(activo)
                .build();

        entityManager.persist(beneficio);
        entityManager.flush();

        assertNotNull(beneficio.getIdBeneficio(), "la siembra del beneficio no entro");

        return beneficio;
    }

    /** Una asociacion activa entre la parte de la ficha dada y el beneficio. */
    private ParteBeneficio asociarA(Ficha ficha, Beneficio beneficio) {
        ParteBeneficio asociacion = ParteBeneficio.builder()
                .id(new ParteBeneficioId(
                        ficha.parte().getIdEspecieParte(), beneficio.getIdBeneficio()))
                .especieParteComestible(ficha.parte())
                .beneficio(beneficio)
                .observacion("Asociación sembrada por las pruebas de baja lógica.")
                .activa(true)
                .build();

        entityManager.persist(asociacion);
        entityManager.flush();

        assertEquals(true, asociacion.getActiva(), "la asociación debía nacer activa");

        return asociacion;
    }

    private String tokenCon(String... permisos) {
        UsuarioResponse usuario = UsuarioResponse.builder()
                .id(idDeUsuarioActivo())
                .nombres("Prueba")
                .apellidos("BeneficioBajaLogica")
                // Dominio con punto por el CHECK ck_usuario_correo.
                .correo("prueba.beneficiobaja@prueba.local")
                .nombreUsuario("prueba.beneficiobaja")
                .activo(true)
                .build();

        return "Bearer " + jwtService.emitir(usuario, List.of("ADMINISTRADOR"), List.of(permisos)).token();
    }

    /**
     * El token de estas pruebas firma a nombre de una cuenta real y activa,
     * porque {@code VerificacionDeCuentaActiva} revalida la cuenta del sub en
     * cada peticion autenticada. Ver {@link UsuariosDePrueba}.
     */
    @Autowired
    private UsuarioRepository usuarioRepositorioDePruebas;

    private Long idDeUsuarioActivo() {
        return UsuariosDePrueba.idActivo(usuarioRepositorioDePruebas);
    }
}
