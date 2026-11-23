package sv.edu.ues.fmp.flora.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import sv.edu.ues.fmp.flora.entity.AporteNutricional;
import sv.edu.ues.fmp.flora.entity.enums.EstadoPublicacion;
import sv.edu.ues.fmp.flora.repository.UsuarioRepository;

/**
 * La baja logica de {@code aporte_nutricional}: que el DELETE retire el aporte
 * en lugar de borrarlo, y que retirarlo lo saque de la ficha publica.
 *
 * <h2>Que cambio y por que</h2>
 * {@code DELETE /api/aportes-nutricionales/{id}} borraba la fila, y el GET
 * posterior respondia 404. Era la unica pieza del catalogo que seguia
 * funcionando asi --{@code habitat}, {@code parte_planta}, {@code nutriente},
 * {@code fuente} y, desde el cambio anterior, {@code beneficio} dan de baja
 * con el DELETE y se recuperan con {@code PATCH /{id}/activar}-- y la
 * diferencia costaba dos cosas: se perdia una medicion citada a una fuente, y
 * un borrado por error solo se arreglaba volviendo a teclear la cantidad, la
 * unidad y la porcion de referencia. Esta clase fija el contrato nuevo.
 *
 * <h2>El reparto que no es obvio</h2>
 * La bandera no va en el mismo sitio que el filtro, y son dos cosas distintas:
 * <ul>
 *   <li>{@code GET /especie-parte/{id}} es el listado publico y filtra
 *       {@code activo = true} <strong>siempre</strong>, sin bandera. Es la
 *       garantia de que un aporte retirado no se pinta en la ficha; lo cubren
 *       {@link #aporteRetirado_desapareceDeLaFichaPublica()} y
 *       {@link #parteVisibleConTodosSusAportesRetirados_devuelve200Vacio()}.</li>
 *   <li>{@code GET /nutriente/{id}} es el listado administrativo: por omision
 *       muestra los retirados --es desde donde se los reactiva-- y
 *       {@code ?soloActivos=true} los omite. Lo cubre
 *       {@link #aporteRetirado_sigueEnElListadoCompletoYNoEnSoloActivos()}.</li>
 * </ul>
 * No hay {@code GET /api/aportes-nutricionales} global en este modulo, y no se
 * invento uno solo para colgar la bandera.
 *
 * <h2>Cero residuo</h2>
 * {@code @SpringBootTest @Transactional}: la ficha que siembra
 * {@link SiembraDeFichas}, los aportes que siembra esta clase y lo que
 * escriben los DELETE y los PATCH viven en la transaccion de la prueba, y el
 * rollback se lo lleva. No hay commit en ninguna parte. Las porciones de
 * referencia llevan prefijo {@code Zzporcion} y se numeran, porque
 * {@code uk_aporte_parte_nutriente_porcion} es
 * {@code (id_especie_parte, id_nutriente, porcion_referencia)} y dos siembras
 * de la misma prueba chocarian si la repitieran.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AporteNutricionalBajaLogicaTest {

    /** El permiso del DELETE, que es tambien el del PATCH nuevo. */
    private static final String NUTRICION_GESTIONAR = "NUTRICION_GESTIONAR";

    /** El del listado administrativo por nutriente, que ya lo exigia. */
    private static final String ESPECIE_CONSULTAR = "ESPECIE_CONSULTAR";

    /** Un permiso real del proyecto, pero de otro modulo: sirve para el 403. */
    private static final String OTRO_PERMISO = "MULTIMEDIA_GESTIONAR";

    private static final String APORTES = "/api/aportes-nutricionales";

    private static final String POR_PARTE = APORTES + "/especie-parte/";

    private static final String POR_NUTRIENTE = APORTES + "/nutriente/";

    private static final long ID_INEXISTENTE = 999999L;

    /** Distingue las porciones que siembra esta clase de las de cualquier otra. */
    private static final AtomicLong SECUENCIA = new AtomicLong();

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private EntityManager entityManager;

    @MockitoBean private EmailService emailService;

    private Ficha publicada;
    private Ficha borrador;

    @BeforeEach
    void sembrar() {
        SiembraDeFichas siembra = new SiembraDeFichas(entityManager);
        publicada = siembra.sembrar(EstadoPublicacion.PUBLICADA);
        borrador = siembra.sembrar(EstadoPublicacion.BORRADOR);
    }

    // ---------- 1. La migracion ----------

    /**
     * {@code V21} aplico limpio y de la forma que se pidio: columna
     * {@code boolean NOT NULL} con {@code DEFAULT true}, de modo que las filas
     * que ya estaban en el catalogo quedaron activas sin necesidad de un
     * UPDATE de relleno.
     * <p>
     * La segunda consulta mira los datos y no el esquema: ninguna fila quedo
     * en {@code false} ni en NULL. Vale aunque la suite entera haya corrido
     * antes, porque todas las pruebas son transaccionales y ningun
     * {@code activo = false} llega a confirmarse.
     */
    @Test
    void v21_dejoLaColumnaActivoNotNullConDefaultTrue_yLasFilasExistentesActivas() {
        List<?> filas = entityManager.createNativeQuery(
                        "select is_nullable, column_default from information_schema.columns "
                                + "where table_name = 'aporte_nutricional' and column_name = 'activo'")
                .getResultList();

        assertEquals(1, filas.size(), "la columna activo no existe: V21 no se aplico");

        Object[] definicion = (Object[]) filas.get(0);

        assertEquals("NO", definicion[0], "activo deberia ser NOT NULL");
        assertTrue(String.valueOf(definicion[1]).startsWith("true"),
                "activo deberia tener DEFAULT true, y tiene: " + definicion[1]);

        Number retiradasONulas = (Number) entityManager.createNativeQuery(
                        "select count(*) from aporte_nutricional where activo is not true")
                .getSingleResult();

        assertEquals(0L, retiradasONulas.longValue(),
                "las filas que ya existian debian quedar activas tras la migracion");
    }

    // ---------- 2. El DELETE retira, no borra ----------

    /**
     * El cambio de contrato, en su forma mas simple: tras el DELETE el aporte
     * sigue consultandose --200, no 404-- y lo que cambio es su bandera.
     */
    @Test
    void delete_dejaElAporteConActivoFalse_yNoEnUn404() throws Exception {
        Long id = sembrarAporte(publicada, true).getIdAporte();

        mockMvc.perform(delete(APORTES + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, tokenCon(NUTRICION_GESTIONAR)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(APORTES + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idAporte").value(id))
                .andExpect(jsonPath("$.activo").value(false));
    }

    /**
     * El DELETE no pierde la medicion. Es el punto del cambio: la cantidad, la
     * unidad y la porcion de referencia siguen ahi para poder reactivarlas,
     * que es lo que el borrado fisico hacia imposible.
     */
    @Test
    void delete_conservaLaMedicionYSuFuente() throws Exception {
        AporteNutricional aporte = sembrarAporte(publicada, true);
        Long id = aporte.getIdAporte();
        String porcion = aporte.getPorcionReferencia();
        Long idFuente = aporte.getFuente().getIdFuente();

        mockMvc.perform(delete(APORTES + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, tokenCon(NUTRICION_GESTIONAR)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(APORTES + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidadMedida").value("mg"))
                .andExpect(jsonPath("$.porcionReferencia").value(porcion))
                .andExpect(jsonPath("$.idFuente").value(idFuente))
                .andExpect(jsonPath("$.activo").value(false));

        // La cantidad se comprueba contra la fila y no contra el JSON: un
        // numeric(12,4) viaja como 2.5000 y comparar eso con un literal
        // depende de como el proveedor de JsonPath mapee los decimales.
        entityManager.flush();
        entityManager.clear();

        AporteNutricional recargado = entityManager.find(AporteNutricional.class, id);

        assertEquals(0, new BigDecimal("2.5000").compareTo(recargado.getCantidad()),
                "el DELETE no debia tocar la cantidad");
        assertEquals(false, recargado.getActivo(), "el DELETE debia dejar activo = false");
    }

    /** Dos DELETE seguidos: el segundo no es un 404, la fila sigue estando. */
    @Test
    void delete_repetido_sigueSiendo204() throws Exception {
        Long id = sembrarAporte(publicada, true).getIdAporte();

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(delete(APORTES + "/" + id)
                            .header(HttpHeaders.AUTHORIZATION, tokenCon(NUTRICION_GESTIONAR)))
                    .andExpect(status().isNoContent());
        }

        mockMvc.perform(get(APORTES + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    /** El 404 de un id que no existe no cambia. */
    @Test
    void delete_deUnIdInexistente_sigueSiendo404() throws Exception {
        mockMvc.perform(delete(APORTES + "/" + ID_INEXISTENTE)
                        .header(HttpHeaders.AUTHORIZATION, tokenCon(NUTRICION_GESTIONAR)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404));
    }

    // ---------- 3. El PATCH devuelve lo retirado ----------

    @Test
    void patchActivar_devuelveElAporteAlCatalogo() throws Exception {
        AporteNutricional aporte = sembrarAporte(publicada, false);
        Long id = aporte.getIdAporte();
        Long idNutriente = aporte.getNutriente().getIdNutriente();

        mockMvc.perform(patch(APORTES + "/" + id + "/activar")
                        .header(HttpHeaders.AUTHORIZATION, tokenCon(NUTRICION_GESTIONAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idAporte").value(id))
                .andExpect(jsonPath("$.activo").value(true));

        // Y vuelve a salir por las dos puertas de las que el DELETE lo saca.
        mockMvc.perform(get(POR_NUTRIENTE + idNutriente + "?soloActivos=true")
                        .header(HttpHeaders.AUTHORIZATION, tokenCon(ESPECIE_CONSULTAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idAporte == " + id + ")]").isNotEmpty());

        mockMvc.perform(get(POR_PARTE + publicada.parte().getIdEspecieParte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idAporte == " + id + ")]").isNotEmpty());
    }

    /** Idempotente, como sus hermanos del catalogo: 200 y nada que cambiar. */
    @Test
    void patchActivar_dosVeces_sigueSiendo200YActivo() throws Exception {
        Long id = sembrarAporte(publicada, false).getIdAporte();

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(patch(APORTES + "/" + id + "/activar")
                            .header(HttpHeaders.AUTHORIZATION, tokenCon(NUTRICION_GESTIONAR)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.idAporte").value(id))
                    .andExpect(jsonPath("$.activo").value(true));
        }
    }

    /** Sobre uno que nunca se dio de baja tampoco es un error. */
    @Test
    void patchActivar_sobreUnoYaActivo_sigueSiendo200() throws Exception {
        Long id = sembrarAporte(publicada, true).getIdAporte();

        mockMvc.perform(patch(APORTES + "/" + id + "/activar")
                        .header(HttpHeaders.AUTHORIZATION, tokenCon(NUTRICION_GESTIONAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void patchActivar_deUnIdInexistente_es404() throws Exception {
        mockMvc.perform(patch(APORTES + "/" + ID_INEXISTENTE + "/activar")
                        .header(HttpHeaders.AUTHORIZATION, tokenCon(NUTRICION_GESTIONAR)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404));
    }

    // ---------- 4. Retirado no se pinta en la ficha publica ----------

    /**
     * La garantia del cambio. El aporte cuelga de una parte de una ficha
     * PUBLICADA, asi que un anonimo lo ve; tras el DELETE desaparece de ese
     * listado aunque la parte siga visible y activa. Ese filtro no tiene
     * bandera que lo apague.
     */
    @Test
    void aporteRetirado_desapareceDeLaFichaPublica() throws Exception {
        Long id = sembrarAporte(publicada, true).getIdAporte();
        String rutaDeLaParte = POR_PARTE + publicada.parte().getIdEspecieParte();

        // Linea base: con el aporte activo, el anonimo lo ve.
        mockMvc.perform(get(rutaDeLaParte))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idAporte == " + id + ")]").isNotEmpty());

        mockMvc.perform(delete(APORTES + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, tokenCon(NUTRICION_GESTIONAR)))
                .andExpect(status().isNoContent());

        // Y ya no, aunque la parte siga siendo visible y tenga otros aportes.
        mockMvc.perform(get(rutaDeLaParte))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$[?(@.idAporte == " + id + ")]").isEmpty());

        // Ni con ESPECIE_CONSULTAR: el filtro es del renglon, no del permiso.
        mockMvc.perform(get(rutaDeLaParte)
                        .header(HttpHeaders.AUTHORIZATION, tokenCon(ESPECIE_CONSULTAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idAporte == " + id + ")]").isEmpty());
    }

    /**
     * El caso de borde del punto anterior: una parte publicada y visible con
     * todos sus aportes retirados responde 200 con lista vacia, no 404. El 404
     * de esta ruta habla de la parte --que no existe, o que cuelga de una ficha
     * que no se puede ver-- y confundirlo con "no hay nada que mostrar"
     * delataria por omision que esa parte tuvo aportes.
     */
    @Test
    void parteVisibleConTodosSusAportesRetirados_devuelve200Vacio() throws Exception {
        String rutaDeLaParte = POR_PARTE + publicada.parte().getIdEspecieParte();

        // La parte llega con el aporte de la siembra; se le agrega otro para
        // que la lista vacia no pueda venir de que solo hubiera uno.
        List<Long> ids = List.of(
                publicada.aporte().getIdAporte(),
                sembrarAporte(publicada, true).getIdAporte());

        mockMvc.perform(get(rutaDeLaParte))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(ids.size()));

        for (Long id : ids) {
            mockMvc.perform(delete(APORTES + "/" + id)
                            .header(HttpHeaders.AUTHORIZATION, tokenCon(NUTRICION_GESTIONAR)))
                    .andExpect(status().isNoContent());
        }

        mockMvc.perform(get(rutaDeLaParte))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---------- 5. El listado administrativo y su bandera ----------

    /**
     * El reparto del punto 5 y del 6, en una sola prueba: el mismo aporte
     * retirado sale del listado completo por nutriente --con su bandera en
     * false, que es lo que permite reactivarlo-- y no sale de
     * {@code ?soloActivos=true}. La bandera no cambio el valor por omision:
     * sin ella sigue devolviendo todo.
     */
    @Test
    void aporteRetirado_sigueEnElListadoCompletoYNoEnSoloActivos() throws Exception {
        AporteNutricional aporte = sembrarAporte(publicada, true);
        Long id = aporte.getIdAporte();
        String ruta = POR_NUTRIENTE + aporte.getNutriente().getIdNutriente();
        String token = tokenCon(ESPECIE_CONSULTAR, NUTRICION_GESTIONAR);

        // Linea base: activo, sale por las dos.
        for (String conYSinBandera : List.of(ruta, ruta + "?soloActivos=true")) {
            mockMvc.perform(get(conYSinBandera).header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.idAporte == " + id + ")]").isNotEmpty());
        }

        mockMvc.perform(delete(APORTES + "/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        // Sin la bandera sigue saliendo, y con su estado a la vista.
        mockMvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idAporte == " + id
                        + " && @.activo == false)]").isNotEmpty());

        // Con la bandera, no.
        mockMvc.perform(get(ruta + "?soloActivos=true").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idAporte == " + id + ")]").isEmpty());

        // Y ?soloActivos=false es lo mismo que no mandarla.
        mockMvc.perform(get(ruta + "?soloActivos=false").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idAporte == " + id + ")]").isNotEmpty());
    }

    /**
     * Contrapeso. Sin este caso, un filtro roto que no devolviera nunca nada
     * pasaria igual las pruebas de arriba: el aporte activo se lista con
     * normalidad por las tres puertas.
     */
    @Test
    void aporteActivo_seListaConNormalidad() throws Exception {
        AporteNutricional aporte = sembrarAporte(publicada, true);
        Long id = aporte.getIdAporte();

        mockMvc.perform(get(POR_PARTE + publicada.parte().getIdEspecieParte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idAporte == " + id
                        + " && @.activo == true)]").isNotEmpty());

        for (String ruta : List.of(
                POR_NUTRIENTE + aporte.getNutriente().getIdNutriente(),
                POR_NUTRIENTE + aporte.getNutriente().getIdNutriente() + "?soloActivos=true")) {

            mockMvc.perform(get(ruta)
                            .header(HttpHeaders.AUTHORIZATION, tokenCon(ESPECIE_CONSULTAR)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.idAporte == " + id + ")]").isNotEmpty());
        }
    }

    /** Lo que se crea nace activo, y el cuerpo no tiene que decirlo. */
    @Test
    void elAporteSembradoPorDefecto_naceActivo() throws Exception {
        mockMvc.perform(get(APORTES + "/" + publicada.aporte().getIdAporte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
    }

    // ---------- 6. La visibilidad de la ficha sigue intacta ----------

    /**
     * El filtro nuevo se suma al que ya habia, no lo sustituye. La parte de un
     * BORRADOR sigue siendo 404 para el anonimo --no un 200 vacio-- y con
     * {@code ESPECIE_CONSULTAR} se ve su contenido activo. Que el aporte este
     * activo o retirado no cambia el 404: lo decide el estado de la ficha.
     */
    @Test
    void visibilidadIntacta_elBorradorSigueSiendo404YConPermisoSeVe() throws Exception {
        Long idEnBorrador = sembrarAporte(borrador, true).getIdAporte();
        String rutaDelBorrador = POR_PARTE + borrador.parte().getIdEspecieParte();

        // Anonimo: 404, y el cuerpo es el del recurso que no existe.
        mockMvc.perform(get(rutaDelBorrador))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.error").value("Recurso no encontrado"));

        // Token sin ESPECIE_CONSULTAR: tambien 404, no 403.
        mockMvc.perform(get(rutaDelBorrador)
                        .header(HttpHeaders.AUTHORIZATION, tokenCon(NUTRICION_GESTIONAR)))
                .andExpect(status().isNotFound());

        // Con el permiso: se ve lo no publicado, y solo lo activo.
        mockMvc.perform(get(rutaDelBorrador)
                        .header(HttpHeaders.AUTHORIZATION, tokenCon(ESPECIE_CONSULTAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idAporte == " + idEnBorrador + ")]").isNotEmpty());

        // Retirarlo no convierte el 404 en otra cosa, ni al reves.
        mockMvc.perform(delete(APORTES + "/" + idEnBorrador)
                        .header(HttpHeaders.AUTHORIZATION, tokenCon(NUTRICION_GESTIONAR)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(rutaDelBorrador))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(rutaDelBorrador)
                        .header(HttpHeaders.AUTHORIZATION, tokenCon(ESPECIE_CONSULTAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idAporte == " + idEnBorrador + ")]").isEmpty());
    }

    /** Y la parte de la ficha PUBLICADA la sigue viendo cualquiera. */
    @Test
    void visibilidadIntacta_elAnonimoVeLosAportesActivosDeUnaFichaPublicada() throws Exception {
        Long id = sembrarAporte(publicada, true).getIdAporte();

        mockMvc.perform(get(POR_PARTE + publicada.parte().getIdEspecieParte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idAporte == " + id + ")]").isNotEmpty());
    }

    // ---------- 7. El PATCH pide lo mismo que el DELETE ----------

    /**
     * {@code NUTRICION_GESTIONAR} en las dos rutas: quien puede retirar un
     * aporte es quien puede devolverlo. El 401 del anonimo lo pone la matriz
     * de seguridad --{@code CATALOGO_PUBLICO} abre
     * {@code /api/aportes-nutricionales/**} solo para GET, asi que el PATCH
     * nuevo cae en el {@code anyRequest().authenticated()} sin tocar
     * {@code SecurityConfig}-- y el 403 lo pone el {@code @PreAuthorize}.
     */
    @Test
    void patchActivar_exigeElMismoPermisoQueElDelete() throws Exception {
        Long id = sembrarAporte(publicada, true).getIdAporte();

        for (String ruta : List.of(
                APORTES + "/" + id + "/activar",
                APORTES + "/" + id)) {

            boolean esElPatch = ruta.endsWith("/activar");

            mockMvc.perform(esElPatch ? patch(ruta) : delete(ruta))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.estado").value(401));

            mockMvc.perform((esElPatch ? patch(ruta) : delete(ruta))
                            .header(HttpHeaders.AUTHORIZATION, tokenCon(OTRO_PERMISO)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.estado").value(403));

            mockMvc.perform((esElPatch ? patch(ruta) : delete(ruta))
                            .header(HttpHeaders.AUTHORIZATION, tokenCon(NUTRICION_GESTIONAR)))
                    .andExpect(esElPatch ? status().isOk() : status().isNoContent());
        }
    }

    // ---------- Siembra propia de esta clase ----------

    /**
     * Un aporte mas sobre la parte de la ficha dada, con el nutriente y la
     * fuente que ya trae la siembra.
     * <p>
     * La porcion de referencia se numera porque
     * {@code uk_aporte_parte_nutriente_porcion} cubre
     * {@code (id_especie_parte, id_nutriente, porcion_referencia)}: reutilizar
     * el nutriente de la siembra --que es lo que hace practicable la prueba del
     * listado por nutriente-- obliga a que al menos la porcion sea distinta.
     */
    private AporteNutricional sembrarAporte(Ficha ficha, boolean activo) {
        AporteNutricional aporte = AporteNutricional.builder()
                .especieParteComestible(ficha.parte())
                .nutriente(ficha.aporte().getNutriente())
                .fuente(ficha.aporte().getFuente())
                .cantidad(new BigDecimal("2.5000"))
                .unidadMedida("mg")
                .porcionReferencia("Zzporcion " + SECUENCIA.incrementAndGet())
                .observacion("Aporte sembrado por las pruebas de baja lógica.")
                .activo(activo)
                .build();

        entityManager.persist(aporte);
        entityManager.flush();

        assertNotNull(aporte.getIdAporte(), "la siembra del aporte no entro");
        assertEquals(activo, aporte.getActivo(), "el aporte no nacio con el estado pedido");

        return aporte;
    }

    private String tokenCon(String... permisos) {
        UsuarioResponse usuario = UsuarioResponse.builder()
                .id(idDeUsuarioActivo())
                .nombres("Prueba")
                .apellidos("AporteBajaLogica")
                // Dominio con punto por el CHECK ck_usuario_correo.
                .correo("prueba.aportebaja@prueba.local")
                .nombreUsuario("prueba.aportebaja")
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
