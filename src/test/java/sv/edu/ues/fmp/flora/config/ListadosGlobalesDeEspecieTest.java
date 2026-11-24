package sv.edu.ues.fmp.flora.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import sv.edu.ues.fmp.flora.dto.response.UsuarioResponse;
import sv.edu.ues.fmp.flora.repository.UsuarioRepository;
import sv.edu.ues.fmp.flora.service.EmailService;
import sv.edu.ues.fmp.flora.service.JwtService;

/**
 * Pruebas de los cuatro listados <strong>globales</strong> de contenido de
 * especie: los que no reciben el id de ninguna ficha.
 *
 * <h2>Lo que estaba mal</h2>
 * Cerradas las consultas que entran por el id de una especie
 * ({@link EspecieVisibilidadPublicaTest},
 * {@link EspecieSubRecursosVisibilidadTest}) y las que entran por el id de un
 * hijo suyo ({@link EspecieRutasPlanasVisibilidadTest}), quedaban los listados
 * que no entran por ningun id: un {@code findAll()} de preparaciones, dos de
 * parte-beneficio y el cruce por nutriente. Ninguno miraba el estado de la
 * ficha de la que cuelga cada renglon, asi que sin token devolvian tambien el
 * contenido de los BORRADOR y EN_REVISION: justo el que las rutas por padre ya
 * habian dejado de mostrar.
 *
 * <h2>Por que aqui es 401 y no 404</h2>
 * En las rutas planas la respuesta tenia que ser 404 porque un 403 sobre el id
 * de una imagen confirmaba que esa imagen existe. Un listado no se pide por el
 * id de nada: cortarlo en el filtro no delata ninguna fila, asi que estos
 * cuatro se cierran en la matriz de {@link SecurityConfig} --quedan fuera de
 * {@code CATALOGO_PUBLICO}, recortados por {@code LISTADOS_GLOBALES}-- mas un
 * {@code @PreAuthorize} en el metodo. De ahi los tres codigos que se afirman:
 * 401 sin token, 403 con token sin el permiso y 200 con
 * {@code ESPECIE_CONSULTAR}.
 *
 * <h2>El quinto se fue, y por que</h2>
 * {@code GET /api/epocas-cosecha} estaba aqui y ya no: era el <strong>unico
 * listado de epocas de cosecha</strong>, asi que cerrarlo con el permiso dejo
 * al catalogo publico sin un sub-recurso de las fichas PUBLICADAS --un anonimo
 * no podia pintar la epoca de cosecha de una especie publicada--. Y no era un
 * listado global de verdad: recibe el id de una parte comestible, igual que
 * {@code /api/preparaciones-consumo/parte/{id}}. Lo que le faltaba era el
 * filtro, no el permiso: {@code EpocaCosechaServiceImpl.listarPorParte} era el
 * unico hermano sin {@code VisibilidadDeEspecie} y ahora lo aplica. Esta clase
 * conserva una sola afirmacion sobre esa ruta --que el anonimo <em>llega</em>--
 * y lo que se ve o no se ve por ella lo cubre
 * {@link EspecieRutasPlanasVisibilidadTest}, con sus hermanas de id de padre.
 *
 * <h2>Por que ninguna prueba necesita datos</h2>
 * Un listado contesta 200 con lista vacia, asi que no hace falta sembrar nada
 * para medir el guard. La excepcion era {@code GET /api/epocas-cosecha}, que
 * exige el id de una parte comestible y responde 404 si no existe; con esa ruta
 * fuera, la siembra que la sostenia se fue con ella.
 */
@SpringBootTest
@AutoConfigureMockMvc
// Solo lecturas; el rollback deja constancia de que estas pruebas no tocan los
// datos sembrados que usan.
@Transactional
class ListadosGlobalesDeEspecieTest {

    private static final String ESPECIE_CONSULTAR = "ESPECIE_CONSULTAR";

    /** Un permiso real del proyecto, pero de otro modulo: sirve para el 403. */
    private static final String OTRO_PERMISO = "MULTIMEDIA_GESTIONAR";

    private static final String ID_INEXISTENTE = "999999";

    private static final String PREPARACIONES = "/api/preparaciones-consumo";
    private static final String PARTE_BENEFICIOS = "/api/parte-beneficios";
    private static final String PARTE_BENEFICIOS_ACTIVAS = "/api/parte-beneficios/activas";
    private static final String APORTES_POR_NUTRIENTE =
            "/api/aportes-nutricionales/nutriente/" + ID_INEXISTENTE;
    private static final String EPOCAS = "/api/epocas-cosecha";

    /** Los cuatro cerrados. Ninguno necesita parametros. */
    private static final List<String> SIN_PARAMETROS = List.of(
            PREPARACIONES,
            PARTE_BENEFICIOS,
            PARTE_BENEFICIOS_ACTIVAS,
            APORTES_POR_NUTRIENTE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private EmailService emailService;

    private String tokenCon(String... permisos) {
        UsuarioResponse usuario = UsuarioResponse.builder()
                .id(idDeUsuarioActivo())
                .nombres("Prueba")
                .apellidos("ListadosGlobales")
                .correo("prueba.listadosglobales@local")
                .nombreUsuario("prueba.listadosglobales")
                .activo(true)
                .build();

        return "Bearer " + jwtService.emitir(usuario, List.of("CONSULTOR"), List.of(permisos)).token();
    }

    // ---------- El anonimo ya no llega ----------

    @Test
    void listadosGlobales_sinToken_devuelven401() throws Exception {
        for (String ruta : SIN_PARAMETROS) {
            mockMvc.perform(get(ruta))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.estado").value(401));
        }
    }

    // ---------- Con token, pero sin el permiso ----------

    @Test
    void listadosGlobales_conTokenSinEspecieConsultar_devuelven403() throws Exception {
        for (String ruta : SIN_PARAMETROS) {
            mockMvc.perform(get(ruta)
                            .header(HttpHeaders.AUTHORIZATION, tokenCon(OTRO_PERMISO)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.estado").value(403));
        }
    }

    // ---------- Con ESPECIE_CONSULTAR ----------

    @Test
    void listadosGlobales_conEspecieConsultar_devuelven200() throws Exception {
        for (String ruta : SIN_PARAMETROS) {
            mockMvc.perform(get(ruta)
                            .header(HttpHeaders.AUTHORIZATION, tokenCon(ESPECIE_CONSULTAR)))
                    .andExpect(status().isOk());
        }
    }

    // ---------- Lo que seguia abierto sigue abierto ----------

    /**
     * El recorte de {@code CATALOGO_PUBLICO} toca cuatro rutas exactas, no las
     * familias enteras. Los hermanos de estos listados que si reciben el id del
     * padre --y que por eso ya filtran por el estado de la ficha dentro del
     * servicio-- tienen que seguir atendiendo al anonimo. Lo que se mide es que
     * no sean 401 ni 403: con el id 999999 lo que corresponde es la respuesta
     * de cada servicio, no un corte de seguridad.
     */
    @Test
    void losListadosPorPadre_siguenAbiertosSinToken() throws Exception {
        mockMvc.perform(get(PREPARACIONES + "/parte/" + ID_INEXISTENTE))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(PARTE_BENEFICIOS + "/" + ID_INEXISTENTE + "/" + ID_INEXISTENTE))
                .andExpect(status().isNotFound());

        // El listado por parte de parte_beneficio, la ruta que el modulo no
        // tenia: cae bajo el comodin /api/parte-beneficios/** de
        // CATALOGO_PUBLICO y los dos recortes de LISTADOS_GLOBALES son rutas
        // exactas, asi que no la alcanzan. El 404 es la respuesta del servicio
        // a una parte que no existe: el anonimo llego al metodo, que es lo que
        // esta prueba fija. Lo que se ve o no por ella lo cubren
        // EspecieRutasPlanasVisibilidadTest y ParteBeneficiosPorParteTest.
        mockMvc.perform(get(PARTE_BENEFICIOS + "/parte/" + ID_INEXISTENTE))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(EPOCAS + "/" + ID_INEXISTENTE))
                .andExpect(status().isNotFound());

        // El listado de epocas, el que salio de LISTADOS_GLOBALES. El 404 es la
        // respuesta del servicio a una parte que no existe: el anonimo llego al
        // metodo, que es lo que esta prueba fija. Que la parte de una ficha sin
        // publicar devuelva ese mismo 404 lo comprueba
        // EspecieRutasPlanasVisibilidadTest.
        mockMvc.perform(get(EPOCAS).param("idEspecieParte", ID_INEXISTENTE))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/aportes-nutricionales/" + ID_INEXISTENTE))
                .andExpect(status().isNotFound());

        // Alineado con sus hermanos: la parte que no existe es 404. El anonimo
        // llego al metodo, que es lo que esta prueba fija; el borrador lo cubre
        // EspecieRutasPlanasVisibilidadTest.
        mockMvc.perform(get("/api/aportes-nutricionales/especie-parte/" + ID_INEXISTENTE))
                .andExpect(status().isNotFound());
    }

    /** Los catalogos que no cuelgan de ninguna especie no entraron en el cambio. */
    @Test
    void catalogosIndependientes_siguenAbiertosSinToken() throws Exception {
        mockMvc.perform(get("/api/nutrientes")).andExpect(status().isOk());
        mockMvc.perform(get("/api/beneficios")).andExpect(status().isOk());
        mockMvc.perform(get("/api/partes-planta")).andExpect(status().isOk());
    }

    /**
     * El token de estas pruebas firma a nombre de una cuenta real y activa.
     * <p>
     * Antes iba un id inventado (999): daba igual, porque nadie consultaba al
     * usuario. Con {@code VerificacionDeCuentaActiva} en la cadena ese token
     * recibe 401, y con razon --es el mismo caso que un token de una cuenta
     * borrada--. Ver {@link sv.edu.ues.fmp.flora.config.UsuariosDePrueba}.
     */
    @Autowired
    private UsuarioRepository usuarioRepositorioDePruebas;

    private Long idDeUsuarioActivo() {
        return UsuariosDePrueba.idActivo(usuarioRepositorioDePruebas);
    }
}
