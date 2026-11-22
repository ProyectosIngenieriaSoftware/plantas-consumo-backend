package sv.edu.ues.fmp.flora.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import sv.edu.ues.fmp.flora.config.SiembraDeFichas.Ficha;
import sv.edu.ues.fmp.flora.dto.response.UsuarioResponse;
import sv.edu.ues.fmp.flora.entity.Beneficio;
import sv.edu.ues.fmp.flora.entity.EspecieParteComestible;
import sv.edu.ues.fmp.flora.entity.ParteBeneficio;
import sv.edu.ues.fmp.flora.entity.PartePlanta;
import sv.edu.ues.fmp.flora.entity.enums.EstadoPublicacion;
import sv.edu.ues.fmp.flora.entity.enums.ParteBeneficioId;
import sv.edu.ues.fmp.flora.entity.enums.TipoBeneficio;
import sv.edu.ues.fmp.flora.service.EmailService;
import sv.edu.ues.fmp.flora.service.JwtService;

/**
 * Pruebas de la ruta que faltaba: {@code GET /api/parte-beneficios/parte/{id}}.
 *
 * <h2>Que hueco llena la ruta</h2>
 * La ficha publica se arma con {@code GET /api/especies/publicadas/{id}} mas
 * las rutas planas de cada sub-recurso, pero no tenia manera de listar los
 * beneficios de una parte comestible. El modulo solo traia dos listados
 * --{@code GET /api/parte-beneficios} y {@code /activas}-- y los dos son
 * globales: recorren el contenido de todas las especies sin mirar el estado de
 * ninguna ficha, asi que exigen {@code ESPECIE_CONSULTAR} con razon y estan
 * recortados de lo publico en {@code LISTADOS_GLOBALES}. Lo unico que quedaba
 * abierto era {@code GET /api/parte-beneficios/{idParte}/{idBeneficio}}, que
 * obliga a saber el {@code idBeneficio} de antemano: el front tendria que
 * adivinarlo. Faltaba el listado por padre, el hermano de
 * {@code /api/preparaciones-consumo/parte/{id}}.
 *
 * <h2>Por que la ruta nueva puede estar abierta</h2>
 * Porque recibe el id de la parte, asi que
 * {@code ParteBeneficioServiceImpl.listarPorParte} puede preguntarle a
 * {@code VisibilidadDeEspecie} por el estado de la ficha de la que cuelga, y
 * responder el 404 de una parte inexistente cuando la respuesta es no. Es el
 * mismo arreglo que se le hizo a {@code EpocaCosechaServiceImpl.listarPorParte}.
 *
 * <h2>Lo que se mide aqui y lo que se mide al lado</h2>
 * {@link EspecieRutasPlanasVisibilidadTest} mete esta ruta en su familia de
 * casos y con eso cubre las cuatro afirmaciones comunes --404 al anonimo sobre
 * un borrador, mismo cuerpo que un id inventado, 200 sobre una publicada y 200
 * con el permiso-- en los tres estados. Esta clase cubre lo que es propio de
 * esta ruta y no encaja en ese bucle: el <strong>contenido</strong> de la
 * lista (que traiga el beneficio sembrado), la parte visible <strong>sin</strong>
 * beneficios (200 con lista vacia, no 404), el criterio de estado (una
 * asociacion inactiva no sale) y el contrapeso de que los dos listados globales
 * no se abrieron de rebote.
 *
 * <h2>Cero residuo</h2>
 * {@code @SpringBootTest @Transactional}: lo que siembra {@link SiembraDeFichas}
 * y lo que siembra esta clase viven en la transaccion de la prueba y el
 * rollback se lo lleva. No hay commit en ninguna parte.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ParteBeneficiosPorParteTest {

    private static final String ESPECIE_CONSULTAR = "ESPECIE_CONSULTAR";

    /** Un permiso real del proyecto, pero de otro modulo: sirve para el 403. */
    private static final String OTRO_PERMISO = "MULTIMEDIA_GESTIONAR";

    private static final long ID_INEXISTENTE = 999999L;

    private static final String POR_PARTE = "/api/parte-beneficios/parte/";

    /** Distingue los nombres que siembra esta clase de los de cualquier otra. */
    private static final AtomicLong SECUENCIA = new AtomicLong();

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private EntityManager entityManager;

    @MockitoBean private EmailService emailService;

    private Ficha borrador;
    private Ficha enRevision;
    private Ficha publicada;

    @BeforeEach
    void sembrar() {
        SiembraDeFichas siembra = new SiembraDeFichas(entityManager);

        borrador = siembra.sembrar(EstadoPublicacion.BORRADOR);
        enRevision = siembra.sembrar(EstadoPublicacion.EN_REVISION);
        publicada = siembra.sembrar(EstadoPublicacion.PUBLICADA);
    }

    private List<Ficha> sinPublicar() {
        return List.of(borrador, enRevision);
    }

    private String tokenCon(String... permisos) {
        UsuarioResponse usuario = UsuarioResponse.builder()
                .id(999L)
                .nombres("Prueba")
                .apellidos("ParteBeneficiosPorParte")
                // Dominio con punto por el CHECK ck_usuario_correo.
                .correo("prueba.partebeneficios@prueba.local")
                .nombreUsuario("prueba.partebeneficios")
                .activo(true)
                .build();

        return "Bearer " + jwtService.emitir(usuario, List.of("CONSULTOR"), List.of(permisos)).token();
    }

    private static String rutaDe(Ficha ficha) {
        return POR_PARTE + ficha.parte().getIdEspecieParte();
    }

    // ---------- 1. La ficha publica ya puede pintar los beneficios ----------

    /**
     * El caso que motivo la ruta: sin token, la parte de una ficha PUBLICADA
     * devuelve sus beneficios, con el {@code idBeneficio} dentro --que es justo
     * lo que el front no podia adivinar--.
     */
    @Test
    void anonimo_partePublicada_devuelveSusBeneficios() throws Exception {
        ParteBeneficio sembrado = publicada.parteBeneficio();

        mockMvc.perform(get(rutaDe(publicada)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idEspecieParte")
                        .value(sembrado.getId().getIdEspecieParte()))
                .andExpect(jsonPath("$[0].idBeneficio")
                        .value(sembrado.getId().getIdBeneficio()))
                .andExpect(jsonPath("$[0].observacion").value(sembrado.getObservacion()))
                .andExpect(jsonPath("$[0].activa").value(true));
    }

    // ---------- 2. El anonimo no ve los de un borrador, y no lo nota ----------

    @Test
    void anonimo_parteDeUnaFichaSinPublicar_devuelve404() throws Exception {
        for (Ficha ficha : sinPublicar()) {
            mockMvc.perform(get(rutaDe(ficha)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.estado").value(404))
                    .andExpect(jsonPath("$.error").value("Recurso no encontrado"));
        }
    }

    /**
     * La prueba del oraculo: el cuerpo del 404 por la parte de un borrador
     * tiene que ser identico al de un {@code idEspecieParte} inventado. Si
     * difirieran, la diferencia diria que ese id existe y que su ficha no esta
     * publicada, que es el sondeo que se quiere cerrar.
     */
    @Test
    void anonimo_laParteDeUnBorradorYUnIdInventadoRespondenIgual() throws Exception {
        String delInventado = cuerpoDe(POR_PARTE + ID_INEXISTENTE);

        for (Ficha ficha : sinPublicar()) {
            assertEquals(
                    normalizar(delInventado),
                    normalizar(cuerpoDe(rutaDe(ficha))),
                    "el 404 de la parte de una ficha " + ficha.estado()
                            + " y el de un idEspecieParte inventado difieren");
        }
    }

    /** Un token sin el permiso recibe 404, no 403: el 403 delataria el id. */
    @Test
    void conTokenSinEspecieConsultar_parteDeUnBorrador_devuelve404YNo403() throws Exception {
        for (Ficha ficha : sinPublicar()) {
            mockMvc.perform(get(rutaDe(ficha))
                            .header(HttpHeaders.AUTHORIZATION, tokenCon(OTRO_PERMISO)))
                    .andExpect(status().isNotFound());
        }
    }

    // ---------- 3. Con ESPECIE_CONSULTAR si se ve lo no publicado ----------

    @Test
    void conEspecieConsultar_parteDeUnaFichaSinPublicar_devuelve200() throws Exception {
        for (Ficha ficha : sinPublicar()) {
            mockMvc.perform(get(rutaDe(ficha))
                            .header(HttpHeaders.AUTHORIZATION, tokenCon(ESPECIE_CONSULTAR)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].idBeneficio")
                            .value(ficha.parteBeneficio().getId().getIdBeneficio()));
        }
    }

    // ---------- 4. La parte visible sin beneficios: 200 vacio, no 404 ----------

    /**
     * La diferencia que le importa al front: "esta parte no tiene beneficios"
     * es una lista vacia con 200, no un 404. El 404 esta reservado para la
     * parte que no existe y para la que no se puede ver, que son lo mismo
     * desde fuera.
     */
    @Test
    void partePublicadaSinBeneficios_devuelve200ConListaVacia() throws Exception {
        EspecieParteComestible parteSinBeneficios = sembrarParteSinBeneficios();

        mockMvc.perform(get(POR_PARTE + parteSinBeneficios.getIdEspecieParte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---------- 5. El criterio de estado: lo inactivo no sale ----------

    /**
     * La ruta sigue el mismo criterio que {@code GET /api/parte-beneficios/activas},
     * el unico de los dos listados globales que mira el estado: solo
     * {@code activa = true}. Una asociacion dada de baja no vuelve al catalogo
     * por esta puerta; se sigue viendo por los listados administrativos y por
     * {@code GET /api/parte-beneficios/{idParte}/{idBeneficio}}.
     */
    @Test
    void beneficioInactivo_noApareceEnLaListaPublica() throws Exception {
        ParteBeneficio inactivo = sembrarBeneficioInactivoEn(publicada.parte());
        Long idActivo = publicada.parteBeneficio().getId().getIdBeneficio();
        Long idInactivo = inactivo.getId().getIdBeneficio();

        mockMvc.perform(get(rutaDe(publicada)))
                .andExpect(status().isOk())
                // Solo el activo: si el filtro se cayera, serian dos.
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idBeneficio").value(idActivo))
                .andExpect(jsonPath("$[?(@.idBeneficio == " + idInactivo + ")]").isEmpty());

        // El registro existe: el que no lo devuelva esta ruta es el filtro de
        // estado, no que la siembra no haya entrado.
        mockMvc.perform(get("/api/parte-beneficios/"
                        + inactivo.getId().getIdEspecieParte() + "/" + idInactivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false));
    }

    // ---------- 6. Contrapeso: los dos globales no se abrieron ----------

    /**
     * Lo que <strong>no</strong> cambio. La ruta nueva cae bajo el comodin
     * {@code /api/parte-beneficios/**} de {@code CATALOGO_PUBLICO}, asi que
     * conviene fijar aqui que el recorte de {@code LISTADOS_GLOBALES} --dos
     * rutas exactas-- sigue cerrando los dos listados globales. Si alguien
     * abriera la familia entera por error, esta prueba lo dice.
     */
    @Test
    void losDosListadosGlobales_siguenExigiendoEspecieConsultar() throws Exception {
        List<String> globales = List.of(
                "/api/parte-beneficios",
                "/api/parte-beneficios/activas");

        for (String ruta : globales) {
            mockMvc.perform(get(ruta))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.estado").value(401));

            mockMvc.perform(get(ruta)
                            .header(HttpHeaders.AUTHORIZATION, tokenCon(OTRO_PERMISO)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.estado").value(403));

            mockMvc.perform(get(ruta)
                            .header(HttpHeaders.AUTHORIZATION, tokenCon(ESPECIE_CONSULTAR)))
                    .andExpect(status().isOk());
        }

        // Y la ruta nueva, al lado de esas dos, si atiende al anonimo.
        mockMvc.perform(get(rutaDe(publicada)))
                .andExpect(status().isOk());
    }

    /**
     * Que {@code /parte/{id}} no se la coma
     * {@code /{idEspecieParte}/{idBeneficio}}: las dos tienen dos segmentos y
     * conviven en el mismo controlador. Si Spring resolviera la nueva con la
     * vieja, {@code "parte"} no convertiria a {@code Long} y saldria un 400.
     */
    @Test
    void laRutaNueva_noChocaConLaDeClaveCompuesta() throws Exception {
        mockMvc.perform(get(rutaDe(publicada)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/parte-beneficios/"
                        + publicada.parteBeneficio().getId().getIdEspecieParte() + "/"
                        + publicada.parteBeneficio().getId().getIdBeneficio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idBeneficio").isNumber());
    }

    // ---------- Siembra propia de esta clase ----------

    /**
     * Una parte comestible mas en la ficha PUBLICADA, sin ningun beneficio.
     * Lleva su propia parte de planta para no chocar con la parte que ya
     * sembro {@link SiembraDeFichas} en esa misma especie.
     */
    private EspecieParteComestible sembrarParteSinBeneficios() {
        long n = SECUENCIA.incrementAndGet();

        PartePlanta partePlanta = entityManager.merge(PartePlanta.builder()
                .nombre("Parte sin beneficios " + n)
                .activo(true)
                .build());

        EspecieParteComestible parte = EspecieParteComestible.builder()
                .especie(publicada.especie())
                .partePlanta(partePlanta)
                .descripcion("Parte sembrada sin beneficios por las pruebas.")
                .activa(true)
                .build();

        entityManager.persist(parte);
        entityManager.flush();

        return parte;
    }

    /** Una asociacion inactiva en la parte dada, con un beneficio propio. */
    private ParteBeneficio sembrarBeneficioInactivoEn(EspecieParteComestible parte) {
        long n = SECUENCIA.incrementAndGet();

        Beneficio beneficio = Beneficio.builder()
                .nombre("Beneficio inactivo de prueba " + n)
                .tipoBeneficio(TipoBeneficio.OTRO)
                .activo(true)
                .build();

        entityManager.persist(beneficio);
        entityManager.flush();

        ParteBeneficio asociacion = ParteBeneficio.builder()
                .id(new ParteBeneficioId(parte.getIdEspecieParte(), beneficio.getIdBeneficio()))
                .especieParteComestible(parte)
                .beneficio(beneficio)
                .observacion("Asociación inactiva sembrada por las pruebas.")
                .activa(false)
                .build();

        entityManager.persist(asociacion);
        entityManager.flush();

        return asociacion;
    }

    private String cuerpoDe(String ruta) throws Exception {
        return mockMvc.perform(get(ruta))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    /** Borra lo que por fuerza difiere: los ids, la ruta y el instante. */
    private static String normalizar(String cuerpo) {
        return cuerpo
                .replaceAll("[0-9]{1,}", "{n}")
                .replaceAll("\"timestamp\":\"[^\"]*\"", "\"timestamp\":\"{instante}\"")
                .replaceAll("\"ruta\":\"[^\"]*\"", "\"ruta\":\"{ruta}\"");
    }
}
