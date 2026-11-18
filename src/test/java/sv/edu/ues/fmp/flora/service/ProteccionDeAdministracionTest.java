package sv.edu.ues.fmp.flora.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import sv.edu.ues.fmp.flora.dto.response.UsuarioResponse;
import sv.edu.ues.fmp.flora.entity.Rol;
import sv.edu.ues.fmp.flora.entity.Usuario;
import sv.edu.ues.fmp.flora.entity.UsuarioRol;
import sv.edu.ues.fmp.flora.entity.enums.UsuarioRolId;

/**
 * Red de regresion de SEG-H09: las seis reglas que protegen el acceso de
 * administracion.
 *
 * <h2>Que fija</h2>
 * Que con {@code USUARIO_GESTIONAR} ya no alcance para volverse administrador
 * (SEG-H09), que un administrador no pueda dispararse en el pie --desactivarse
 * ni quitarse su propio rol (SEG-148 y SEG-149)--, que un administrador normal
 * no pueda con otro administrador, que al superadministrador no lo toque nadie
 * y que ninguna operacion deje el sistema con cero administradores activos.
 *
 * <h2>R6, la via de edicion</h2>
 * R1-R5 cubrian la baja logica, el rol y la baja del rol: las vias que quitan
 * el acceso. La <strong>edicion</strong> --{@code PUT /api/usuarios/{id}}-- no
 * pasaba por ninguna, y por ahi un administrador normal cambiaba los datos del
 * superadministrador. No lo desactivaba --ese campo no esta en el DTO-- pero
 * mientras el DTO llevo el {@code correo}, reescribirlo y pedir el
 * {@code recuperar-clave} publico entregaba la cuenta: una suplantacion que no
 * tocaba ninguna de las cinco reglas. R6 cierra el lado de "quien puede
 * editar a quien"; el otro lado --que el correo y el nombre de usuario ya no
 * se editen desde una cuenta ajena-- lo cubre
 * {@code CredencialesAutoservicioTest}.
 * <p>
 * Junto con R6 va la marca: {@code es_superadmin} se expone en
 * {@code UsuarioResponse} para que el frontend pueda bloquear la fila
 * protegida, y hay una prueba de que no se puede escribir por el cuerpo de
 * ninguna de las dos vias que guardan un usuario.
 * <p>
 * Esa ultima parte se comprueba por las tres vias que llevan al mismo estado:
 * quitar el rol, dar de baja al usuario y --la que se cerro despues-- dar de
 * baja el rol ADMINISTRADOR mismo con {@code DELETE /api/roles/{id}}, que no
 * toca ninguna fila de {@code usuario_rol} y aun asi degrada a todos los
 * administradores a la vez.
 * <p>
 * Cada bloqueo se comprueba por dos lados: el codigo de respuesta y que la
 * operacion <strong>no se ejecuto</strong>. Lo segundo es lo que no se puede
 * pasar por casualidad: un 403 emitido despues de haber borrado la fila
 * pasaria la primera mitad de la prueba y no la segunda.
 *
 * <h2>Lo que NO se bloqueo</h2>
 * Las tres pruebas del final son el contrapeso: el superadministrador sigue
 * pudiendo promover, degradar y dar de baja; un gestor de usuarios sigue
 * pudiendo asignar cualquier rol que no sea ADMINISTRADOR y dar de baja a
 * cualquiera que no sea administrador. Sin ellas, un cambio que devolviera 403
 * a todo tambien dejaria la clase en verde.
 *
 * <h2>Datos: siembra propia, sin residuo</h2>
 * Misma infraestructura que las pruebas de H2: {@code @SpringBootTest} con
 * {@code @Transactional}, y todo lo que hace falta --seis usuarios, dos roles y
 * sus filas de {@code usuario_rol}-- se crea aqui dentro, en la transaccion de
 * la prueba, con el {@link EntityManager}. El rollback se lo lleva: no hay
 * {@code commit}, ni {@code @Commit}, ni {@code TransactionTemplate}. El unico
 * rastro es el avance de las secuencias de id, inevitable con
 * {@code GenerationType.IDENTITY}.
 * <p>
 * Nada se busca entre los datos sembrados por las migraciones. Es a proposito:
 * las pruebas no pueden depender de que la base de quien las corra tenga
 * {@code admin} y {@code admin.demo}, ni de cuantos administradores haya en
 * ella; el {@code es_superadmin} de {@code V19} no se mira desde aqui, se
 * siembran los superadmins que la prueba necesita.
 *
 * <h2>Por que el superadmin sembrado no tiene el rol ADMINISTRADOR</h2>
 * La marca {@code es_superadmin} y la fila de {@code usuario_rol} son dos cosas
 * independientes: en florav1 el usuario {@code admin} tiene las dos, pero el
 * superadmin de esta clase se siembra solo con la marca. Asi no entra en la
 * cuenta de "administradores activos" de R5 y las dos pruebas del invariante
 * pueden dejar un unico administrador activo --el objetivo-- y comprobar el
 * 409. Si tambien tuviera el rol, siempre quedaria el como administrador y el
 * invariante no se podria provocar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProteccionDeAdministracionTest {

    private static final String USUARIO_GESTIONAR = "USUARIO_GESTIONAR";
    private static final String ADMINISTRADOR = "ADMINISTRADOR";

    /** Para que los nombres de usuario y de rol sembrados no choquen entre si. */
    private static final AtomicLong SECUENCIA = new AtomicLong();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EntityManager entityManager;

    /** Mismo motivo que en las demas pruebas de contexto completo: lo arrastra. */
    @MockitoBean
    private EmailService emailService;

    private Rol rolAdministrador;

    /** Un rol cualquiera que no es el de administracion, para el contrapeso. */
    private Rol rolSinPrivilegios;

    /** Marca es_superadmin y nada mas: ver el encabezado de la clase. */
    private Usuario superadmin;

    /** Otro superadmin, el objetivo de las pruebas de R4. */
    private Usuario otroSuperadmin;

    /** Administrador normal: rol ADMINISTRADOR vigente, sin la marca. */
    private Usuario administrador;

    /** Un segundo administrador normal, para que R5 no se dispare sin querer. */
    private Usuario otroAdministrador;

    /** Tiene USUARIO_GESTIONAR pero no es administrador: el de la auto-promocion. */
    private Usuario gestor;

    /** Sin roles. El que si se puede dar de baja. */
    private Usuario comun;

    @BeforeEach
    void sembrar() {
        rolAdministrador = rolAdministrador();
        rolSinPrivilegios = rolNuevo("QA_SEG_H09_SIN_PRIVILEGIOS");

        superadmin = crearUsuario("Super", "Admin", true);
        otroSuperadmin = crearUsuario("Otro", "Super", true);

        administrador = crearUsuario("Admin", "Normal", false);
        asignarRol(administrador, rolAdministrador);

        otroAdministrador = crearUsuario("Admin", "Respaldo", false);
        asignarRol(otroAdministrador, rolAdministrador);

        // El otro superadmin si lleva el rol: las pruebas de R4 tienen que
        // poder intentar quitarselo.
        asignarRol(otroSuperadmin, rolAdministrador);

        gestor = crearUsuario("Gestor", "DeUsuarios", false);
        comun = crearUsuario("Usuario", "Comun", false);

        entityManager.flush();
    }

    // ---------- R1: autoproteccion (SEG-148 y SEG-149) ----------

    /** SEG-148: un administrador no puede darse de baja a si mismo. */
    @Test
    void administrador_intentaDesactivarseASiMismo_devuelve403YSigueActivo() throws Exception {
        mockMvc.perform(delete("/api/usuarios/" + administrador.getIdUsuario())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador, USUARIO_GESTIONAR)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("Prohibido"))
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertTrue(recargar(administrador).getActivo(),
                "la cuenta no debia quedar desactivada");
    }

    /**
     * SEG-149: un administrador no puede quitarse su propio rol ADMINISTRADOR.
     * <p>
     * Es la otra mitad de SEG-148 y el motivo de que R1 exista: sin ella, el
     * administrador que se degrada a si mismo deja el sistema con un
     * administrador menos y, si era el ultimo, sin ninguno.
     */
    @Test
    void administrador_intentaQuitarseSuPropioRol_devuelve403YConservaElRol() throws Exception {
        mockMvc.perform(delete(rutaDeQuitar(administrador, rolAdministrador))
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador, USUARIO_GESTIONAR)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403));

        assertTrue(tieneElRol(administrador, rolAdministrador),
                "la fila de usuario_rol no debia borrarse");
    }

    /**
     * R1 no es solo para administradores: nadie se desactiva a si mismo, ni
     * siquiera el superadministrador. Y le responde R1 y no R4, que es el orden
     * que pide la tarea: el mensaje habla de su propia cuenta.
     */
    @Test
    void superadmin_intentaDesactivarseASiMismo_devuelve403() throws Exception {
        mockMvc.perform(delete("/api/usuarios/" + superadmin.getIdUsuario())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(superadmin, USUARIO_GESTIONAR)))
                .andExpect(status().isForbidden());

        assertTrue(recargar(superadmin).getActivo(), "la cuenta no debia quedar desactivada");
    }

    // ---------- R2: solo el superadmin asigna ADMINISTRADOR ----------

    /**
     * La auto-promocion, que es el agujero central de SEG-H09: un usuario con
     * {@code USUARIO_GESTIONAR} se asignaba ADMINISTRADOR y quedaba
     * administrador.
     */
    @Test
    void gestor_intentaAsignarseElRolAdministrador_devuelve403YNoSePromueve() throws Exception {
        mockMvc.perform(post("/api/usuario-roles")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(gestor, USUARIO_GESTIONAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeAsignacion(gestor, rolAdministrador)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403));

        assertFalse(tieneElRol(gestor, rolAdministrador),
                "no debia quedar con el rol ADMINISTRADOR");
    }

    /** Tampoco se lo puede dar a un tercero: R2 no distingue a quien. */
    @Test
    void gestor_intentaPromoverAOtroUsuario_devuelve403() throws Exception {
        mockMvc.perform(post("/api/usuario-roles")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(gestor, USUARIO_GESTIONAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeAsignacion(comun, rolAdministrador)))
                .andExpect(status().isForbidden());

        assertFalse(tieneElRol(comun, rolAdministrador), "el tercero no debia quedar promovido");
    }

    /** Un administrador normal tampoco promueve: ser admin no es ser superadmin. */
    @Test
    void administrador_intentaPromoverAOtroUsuario_devuelve403() throws Exception {
        mockMvc.perform(post("/api/usuario-roles")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador, USUARIO_GESTIONAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeAsignacion(comun, rolAdministrador)))
                .andExpect(status().isForbidden());

        assertFalse(tieneElRol(comun, rolAdministrador), "el tercero no debia quedar promovido");
    }

    // ---------- R3: un admin normal no puede con otro admin ----------

    @Test
    void administrador_intentaQuitarElRolAOtroAdministrador_devuelve403() throws Exception {
        mockMvc.perform(delete(rutaDeQuitar(otroAdministrador, rolAdministrador))
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador, USUARIO_GESTIONAR)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403));

        assertTrue(tieneElRol(otroAdministrador, rolAdministrador),
                "la fila de usuario_rol no debia borrarse");
    }

    @Test
    void administrador_intentaDesactivarAOtroAdministrador_devuelve403() throws Exception {
        mockMvc.perform(delete("/api/usuarios/" + otroAdministrador.getIdUsuario())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador, USUARIO_GESTIONAR)))
                .andExpect(status().isForbidden());

        assertTrue(recargar(otroAdministrador).getActivo(),
                "el otro administrador no debia quedar desactivado");
    }

    // ---------- R4: al superadmin no lo toca nadie ----------

    @Test
    void superadmin_intentaQuitarElRolAOtroSuperadmin_devuelve403() throws Exception {
        mockMvc.perform(delete(rutaDeQuitar(otroSuperadmin, rolAdministrador))
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(superadmin, USUARIO_GESTIONAR)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403));

        assertTrue(tieneElRol(otroSuperadmin, rolAdministrador),
                "al superadministrador no se le quita el rol");
    }

    @Test
    void superadmin_intentaDesactivarAOtroSuperadmin_devuelve403() throws Exception {
        mockMvc.perform(delete("/api/usuarios/" + otroSuperadmin.getIdUsuario())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(superadmin, USUARIO_GESTIONAR)))
                .andExpect(status().isForbidden());

        assertTrue(recargar(otroSuperadmin).getActivo(),
                "la cuenta del superadministrador no se desactiva");
    }

    /** Y un administrador normal menos todavia. */
    @Test
    void administrador_intentaDesactivarAlSuperadmin_devuelve403() throws Exception {
        mockMvc.perform(delete("/api/usuarios/" + otroSuperadmin.getIdUsuario())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador, USUARIO_GESTIONAR)))
                .andExpect(status().isForbidden());

        assertTrue(recargar(otroSuperadmin).getActivo(),
                "la cuenta del superadministrador no se desactiva");
    }

    // ---------- R6: a los datos del superadmin solo los cambia el ----------

    /**
     * La via que no pasaba por ninguna regla: {@code PUT /api/usuarios/{id}}.
     * <p>
     * Un administrador normal no podia ponerle {@code activo = false} al
     * superadministrador --{@code UsuarioUpdateRequest} no tiene ese campo--
     * pero si reescribirle los datos, y mientras el DTO llevaba {@code correo}
     * eso bastaba para quedarse con la cuenta: el {@code recuperar-clave}
     * publico resuelve por correo. Se cerro por los dos lados; esta prueba fija
     * el de R6.
     */
    @Test
    void administrador_intentaEditarAlSuperadmin_devuelve403YNoCambiaNada() throws Exception {
        String nombresAntes = otroSuperadmin.getNombres();

        mockMvc.perform(put("/api/usuarios/" + otroSuperadmin.getIdUsuario())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador, USUARIO_GESTIONAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeEdicion("Intruso", "Reescrito")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("Prohibido"))
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertEquals(nombresAntes, recargar(otroSuperadmin).getNombres(),
                "los datos del superadministrador no debian cambiar");
    }

    /** Ni el gestor de usuarios, que es quien tiene el permiso del endpoint. */
    @Test
    void gestor_intentaEditarAlSuperadmin_devuelve403() throws Exception {
        mockMvc.perform(put("/api/usuarios/" + otroSuperadmin.getIdUsuario())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(gestor, USUARIO_GESTIONAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeEdicion("Intruso", "Reescrito")))
                .andExpect(status().isForbidden());

        assertEquals("Otro", recargar(otroSuperadmin).getNombres(),
                "los datos del superadministrador no debian cambiar");
    }

    /**
     * R6 compara por id, no por la marca de quien llama: un superadministrador
     * no edita a otro, igual que R4 no le deja desactivarlo.
     */
    @Test
    void superadmin_intentaEditarAOtroSuperadmin_devuelve403() throws Exception {
        mockMvc.perform(put("/api/usuarios/" + otroSuperadmin.getIdUsuario())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(superadmin, USUARIO_GESTIONAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeEdicion("Intruso", "Reescrito")))
                .andExpect(status().isForbidden());

        assertEquals("Otro", recargar(otroSuperadmin).getNombres(),
                "los datos del superadministrador no debian cambiar");
    }

    /**
     * El contrapeso de R6: el superadministrador si se edita a si mismo. Sin
     * esta prueba, una regla que prohibiera tambien eso dejaria la clase en
     * verde y la cuenta no podria corregir su propio nombre --y no hay nadie
     * por encima a quien pedirselo--.
     */
    @Test
    void superadmin_seEditaASiMismo_devuelve200YCambia() throws Exception {
        mockMvc.perform(put("/api/usuarios/" + superadmin.getIdUsuario())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(superadmin, USUARIO_GESTIONAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeEdicion("Nombre", "Corregido")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres").value("Nombre"))
                .andExpect(jsonPath("$.apellidos").value("Corregido"));

        assertEquals("Nombre", recargar(superadmin).getNombres(),
                "el superadministrador debia poder corregir su propio nombre");
    }

    /** Editar a cualquiera que no sea superadmin sigue siendo normal. */
    @Test
    void administrador_editaAUnUsuarioComun_devuelve200() throws Exception {
        mockMvc.perform(put("/api/usuarios/" + comun.getIdUsuario())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador, USUARIO_GESTIONAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeEdicion("Editado", "PorAdmin")))
                .andExpect(status().isOk());

        assertEquals("Editado", recargar(comun).getNombres(),
                "la edicion de un usuario comun no debia bloquearse");
    }

    /**
     * Mismo criterio que la baja: R6 se evalua despues de encontrar al usuario,
     * asi que un id inexistente sigue siendo 404 y no 403.
     */
    @Test
    void editarUnIdInexistente_sigueDando404() throws Exception {
        mockMvc.perform(put("/api/usuarios/999999")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador, USUARIO_GESTIONAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeEdicion("Nadie", "NoExiste")))
                .andExpect(status().isNotFound());
    }

    // ---------- La marca es de solo lectura ----------

    /**
     * {@code es_superadmin} sale en la respuesta --el frontend la necesita para
     * bloquear la fila protegida-- y no entra por ningun cuerpo.
     * <p>
     * Se comprueba por las dos vias que escriben un usuario: la edicion y la
     * creacion. Las dos reciben {@code "esSuperadmin": true} y las dos tienen
     * que dejar la marca en false: ningun DTO de entrada la lleva, asi que
     * Jackson la descarta en silencio ({@code FAIL_ON_UNKNOWN_PROPERTIES} esta
     * desactivado). Si algun dia alguien la agregara a un DTO, esta prueba es
     * la que se pone roja.
     */
    @Test
    void laMarcaDeSuperadmin_seLeeEnLaRespuestaYNoSeEscribePorElCuerpo() throws Exception {
        // 1. Se lee, y distingue al superadmin del que no lo es.
        mockMvc.perform(get("/api/usuarios/" + superadmin.getIdUsuario())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador, USUARIO_GESTIONAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esSuperadmin").value(true));

        mockMvc.perform(get("/api/usuarios/" + comun.getIdUsuario())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador, USUARIO_GESTIONAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esSuperadmin").value(false));

        // 2. No se escribe por el PUT de edicion.
        mockMvc.perform(put("/api/usuarios/" + comun.getIdUsuario())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador, USUARIO_GESTIONAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres": "Aspirante",
                                  "apellidos": "ASuperadmin",
                                  "esSuperadmin": true,
                                  "es_superadmin": true,
                                  "activo": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esSuperadmin").value(false))
                // De paso: activo tampoco entra por esta via.
                .andExpect(jsonPath("$.activo").value(true));

        assertFalse(recargar(comun).getEsSuperadmin(),
                "la marca no debia poder escribirse por el cuerpo de la edicion");
        assertTrue(recargar(comun).getActivo(),
                "activo no debia poder escribirse por el cuerpo de la edicion");

        // 3. Ni por el POST de creacion.
        long n = SECUENCIA.incrementAndGet();
        mockMvc.perform(post("/api/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador, USUARIO_GESTIONAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres": "Nuevo",
                                  "apellidos": "ConMarca",
                                  "correo": "seg.h09.marca.%d@prueba.local",
                                  "nombreUsuario": "seg.h09.marca.%d",
                                  "clave": "DePrueba1#",
                                  "esSuperadmin": true
                                }
                                """.formatted(n, n)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.esSuperadmin").value(false));
    }

    // ---------- R5: nunca cero administradores activos ----------

    /**
     * El invariante responde 409 y no 403: no es que quien llama no pueda
     * --es el superadministrador, que si puede-- sino que el estado al que
     * llevaria la operacion no es admisible.
     */
    @Test
    void quitarElRolAlUltimoAdministradorActivo_devuelve409YConservaElRol() throws Exception {
        dejarComoUnicoAdministradorActivo(administrador);

        mockMvc.perform(delete(rutaDeQuitar(administrador, rolAdministrador))
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(superadmin, USUARIO_GESTIONAR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje").exists());

        assertTrue(tieneElRol(administrador, rolAdministrador),
                "la fila de usuario_rol no debia borrarse");
    }

    @Test
    void desactivarAlUltimoAdministradorActivo_devuelve409YSigueActivo() throws Exception {
        dejarComoUnicoAdministradorActivo(administrador);

        mockMvc.perform(delete("/api/usuarios/" + administrador.getIdUsuario())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(superadmin, USUARIO_GESTIONAR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409));

        assertTrue(recargar(administrador).getActivo(),
                "el ultimo administrador activo no debia quedar desactivado");
    }

    // ---------- R5 por la via del rol: DELETE /api/roles/{id} ----------

    /**
     * La sexta via a "cero administradores": no se le quita el rol a nadie ni
     * se da de baja a ningun usuario, se desactiva el rol ADMINISTRADOR y con
     * eso deja de otorgar nada a los que lo tienen asignado. Es la unica que no
     * pasa por {@code /api/usuarios} ni por {@code /api/usuario-roles}, asi que
     * ninguna de las pruebas de arriba la cubria.
     * <p>
     * Como la siembra deja dos administradores activos, aqui no hay que dejar
     * uno solo: la baja del rol se los lleva a todos de una vez, de modo que
     * cualquier administrador activo basta para que el estado resultante sea
     * cero.
     */
    @Test
    void desactivarElRolAdministradorConAdministradoresActivos_devuelve409YElRolSigueActivo()
            throws Exception {

        assertTrue(administradoresActivos() > 0,
                "la siembra debia dejar al menos un administrador activo");

        mockMvc.perform(delete("/api/roles/" + rolAdministrador.getIdRol())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(superadmin, USUARIO_GESTIONAR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertTrue(recargar(rolAdministrador).getActivo(),
                "el rol ADMINISTRADOR no debia quedar desactivado");
    }

    /**
     * Y tampoco lo puede el superadministrador, que es lo que separa este 409
     * de un 403: no es una cuestion de quien llama --el superadmin si puede
     * administrar el catalogo de roles-- sino del estado al que llevaria la
     * operacion. La prueba de arriba ya usa el token del superadmin; esta fija
     * el otro lado, el administrador normal, para que el dia que se agregue la
     * restriccion de "solo el superadmin" se vea cual de las dos cambia.
     */
    @Test
    void administrador_intentaDesactivarElRolAdministrador_devuelve409() throws Exception {
        mockMvc.perform(delete("/api/roles/" + rolAdministrador.getIdRol())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador, USUARIO_GESTIONAR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409));

        assertTrue(recargar(rolAdministrador).getActivo(),
                "el rol ADMINISTRADOR no debia quedar desactivado");
    }

    /**
     * La salvedad del invariante: si no hay ningun administrador activo, la
     * baja del rol no es la que deja el sistema sin administracion --ya estaba
     * asi-- y la operacion sigue siendo valida. Sin esta prueba, un 409
     * incondicional sobre el rol ADMINISTRADOR pasaria igual las dos de arriba.
     */
    @Test
    void desactivarElRolAdministradorSinAdministradoresActivos_devuelve204() throws Exception {
        dejarSinAdministradoresActivos();

        mockMvc.perform(delete("/api/roles/" + rolAdministrador.getIdRol())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(superadmin, USUARIO_GESTIONAR)))
                .andExpect(status().isNoContent());

        assertFalse(recargar(rolAdministrador).getActivo(), "el rol debia quedar desactivado");
    }

    /** El otro contrapeso: dar de baja cualquier otro rol sigue siendo normal. */
    @Test
    void desactivarUnRolQueNoEsAdministrador_devuelve204() throws Exception {
        mockMvc.perform(delete("/api/roles/" + rolSinPrivilegios.getIdRol())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(gestor, USUARIO_GESTIONAR)))
                .andExpect(status().isNoContent());

        assertFalse(recargar(rolSinPrivilegios).getActivo(), "el rol debia quedar desactivado");
    }

    // ---------- El contrapeso: lo que sigue funcionando ----------

    @Test
    void superadmin_quitaElRolAUnAdministradorNormal_devuelve204() throws Exception {
        mockMvc.perform(delete(rutaDeQuitar(administrador, rolAdministrador))
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(superadmin, USUARIO_GESTIONAR)))
                .andExpect(status().isNoContent());

        assertFalse(tieneElRol(administrador, rolAdministrador),
                "el rol debia quedar quitado");
    }

    @Test
    void superadmin_desactivaAUnAdministradorNormal_devuelve204() throws Exception {
        mockMvc.perform(delete("/api/usuarios/" + administrador.getIdUsuario())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(superadmin, USUARIO_GESTIONAR)))
                .andExpect(status().isNoContent());

        assertFalse(recargar(administrador).getActivo(), "la cuenta debia quedar desactivada");
    }

    @Test
    void superadmin_asignaElRolAdministrador_devuelve201() throws Exception {
        mockMvc.perform(post("/api/usuario-roles")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(superadmin, USUARIO_GESTIONAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeAsignacion(comun, rolAdministrador)))
                .andExpect(status().isCreated());

        assertTrue(tieneElRol(comun, rolAdministrador), "el rol debia quedar asignado");
    }

    /**
     * Las reglas solo miran el rol ADMINISTRADOR: cualquier otro se sigue
     * asignando con {@code USUARIO_GESTIONAR} y nada mas.
     */
    @Test
    void gestor_asignaUnRolQueNoEsAdministrador_devuelve201() throws Exception {
        mockMvc.perform(post("/api/usuario-roles")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(gestor, USUARIO_GESTIONAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeAsignacion(comun, rolSinPrivilegios)))
                .andExpect(status().isCreated());

        assertTrue(tieneElRol(comun, rolSinPrivilegios), "el rol debia quedar asignado");
    }

    /** Dar de baja a quien no es administrador sigue siendo una operacion normal. */
    @Test
    void gestor_desactivaAUnUsuarioQueNoEsAdministrador_devuelve204() throws Exception {
        mockMvc.perform(delete("/api/usuarios/" + comun.getIdUsuario())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(gestor, USUARIO_GESTIONAR)))
                .andExpect(status().isNoContent());

        assertFalse(recargar(comun).getActivo(), "la cuenta debia quedar desactivada");
    }

    /**
     * Y un id que no existe sigue siendo 404 y no 403: las reglas hablan de un
     * usuario concreto, asi que se evaluan despues de encontrarlo.
     */
    @Test
    void desactivarUnIdInexistente_sigueDando404() throws Exception {
        mockMvc.perform(delete("/api/usuarios/999999")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(superadmin, USUARIO_GESTIONAR)))
                .andExpect(status().isNotFound());
    }

    // ---------- Siembra y utilidades ----------

    /**
     * Deja al usuario indicado como el unico administrador activo del sistema,
     * desactivando a los demas dentro de la transaccion de la prueba.
     * <p>
     * Hace falta porque la base de desarrollo trae sus propios administradores
     * ({@code admin} y {@code admin.demo}) y el invariante de R5 solo se puede
     * provocar cuando queda uno. El rollback devuelve todo a su sitio.
     */
    private void dejarComoUnicoAdministradorActivo(Usuario elQueQueda) {
        desactivarAdministradoresActivosExcepto(elQueQueda.getIdUsuario());

        assertEquals(1, administradoresActivos(),
                "la siembra debia dejar un unico administrador activo");
    }

    /**
     * Deja el sistema sin ningun administrador activo, para la salvedad del
     * invariante: una operacion que no puede dejar el sistema en cero, porque
     * ya lo esta, no es la que causa el problema.
     * <p>
     * Se desactivan las cuentas y no el rol: el rol tiene que seguir activo
     * para que la prueba pueda intentar darlo de baja.
     */
    private void dejarSinAdministradoresActivos() {
        // Ningun id_usuario es 0 (IDENTITY arranca en 1), asi que no se excluye
        // a nadie: mismo truco que UsuarioRolRepository.SIN_EXCLUIR.
        desactivarAdministradoresActivosExcepto(0L);

        assertEquals(0, administradoresActivos(),
                "la siembra debia dejar cero administradores activos");
    }

    private void desactivarAdministradoresActivosExcepto(long idQueQueda) {
        List<Usuario> demas = entityManager.createQuery("""
                        SELECT DISTINCT ur.usuario FROM UsuarioRol ur
                        WHERE upper(ur.rol.nombre) = :nombreRol
                          AND ur.rol.activo = true
                          AND ur.usuario.activo = true
                          AND ur.usuario.idUsuario <> :idQueQueda
                        """, Usuario.class)
                .setParameter("nombreRol", ADMINISTRADOR)
                .setParameter("idQueQueda", idQueQueda)
                .getResultList();

        demas.forEach(usuario -> usuario.setActivo(false));
        entityManager.flush();
    }

    private long administradoresActivos() {
        return entityManager.createQuery("""
                        SELECT count(DISTINCT ur.usuario.idUsuario) FROM UsuarioRol ur
                        WHERE upper(ur.rol.nombre) = :nombreRol
                          AND ur.rol.activo = true
                          AND ur.usuario.activo = true
                        """, Long.class)
                .setParameter("nombreRol", ADMINISTRADOR)
                .getSingleResult();
    }

    /**
     * El rol ADMINISTRADOR de la base, o uno nuevo si no estuviera.
     * <p>
     * En florav1 existe --lo dejo {@code V12}-- pero crearlo si falta es lo que
     * hace que la clase corra igual en una base recien armada, el mismo criterio
     * de {@code SiembraDeFichas} con los catalogos.
     */
    private Rol rolAdministrador() {
        return entityManager.createQuery(
                        "SELECT r FROM Rol r WHERE upper(r.nombre) = :nombre", Rol.class)
                .setParameter("nombre", ADMINISTRADOR)
                .getResultStream()
                .findFirst()
                .orElseGet(() -> persistir(Rol.builder()
                        .nombre(ADMINISTRADOR)
                        .descripcion("Rol de administracion")
                        .activo(true)
                        .build()));
    }

    /** Un rol nuevo, con nombre unico: {@code rol.nombre} no se puede repetir. */
    private Rol rolNuevo(String prefijo) {
        return persistir(Rol.builder()
                .nombre(prefijo + "_" + SECUENCIA.incrementAndGet())
                .descripcion("Rol sembrado por las pruebas de SEG-H09")
                .activo(true)
                .build());
    }

    private Usuario crearUsuario(String nombres, String apellidos, boolean esSuperadmin) {
        long n = SECUENCIA.incrementAndGet();

        return persistir(Usuario.builder()
                .nombres(nombres)
                .apellidos(apellidos)
                .correo("seg.h09." + n + "@prueba.local")
                .nombreUsuario("seg.h09." + n)
                // No es una clave usable: estos usuarios no inician sesion, el
                // token se emite directamente con JwtService.
                .claveHash("$2a$10$pruebaSegH09SinClaveUtilizable00000000000000000000000")
                .activo(true)
                .esSuperadmin(esSuperadmin)
                .build());
    }

    private void asignarRol(Usuario usuario, Rol rol) {
        persistir(UsuarioRol.builder()
                .id(new UsuarioRolId(usuario.getIdUsuario(), rol.getIdRol()))
                .usuario(usuario)
                .rol(rol)
                .build());
    }

    private <T> T persistir(T entidad) {
        entityManager.persist(entidad);
        return entidad;
    }

    /**
     * El usuario tal como lo dejo la operacion.
     *
     * <h2>Por que sin flush ni clear</h2>
     * La peticion de MockMvc corre en el mismo hilo y en la misma transaccion
     * que la prueba, asi que el servicio trabaja sobre esta misma sesion: la
     * baja logica es un {@code setActivo(false)} sobre la instancia gestionada,
     * la misma que devuelve este {@code find}. Mirarla aqui es ver exactamente
     * lo que la operacion hizo o no hizo.
     * <p>
     * Y no se fuerza un {@code flush}: cuando una regla corta la operacion, el
     * servicio lanza una excepcion que Spring ve dentro de su
     * {@code @Transactional} y que marca la transaccion compartida como
     * rollback-only. Pedirle un flush despues a esa transaccion es pedirle algo
     * que no tiene por que poder hacer, y la prueba fallaria por la mecanica de
     * la transaccion y no por lo que se quiere medir.
     */
    private Usuario recargar(Usuario usuario) {
        return entityManager.find(Usuario.class, usuario.getIdUsuario());
    }

    /** El rol tal como lo dejo la operacion; mismo criterio que el de usuario. */
    private Rol recargar(Rol rol) {
        return entityManager.find(Rol.class, rol.getIdRol());
    }

    /**
     * Si la fila de {@code usuario_rol} sigue (o quedo) ahi. Mismo criterio que
     * {@link #recargar(Usuario)}: se mira la sesion en la que corrio la
     * operacion. Un {@code remove} pendiente de flush hace que
     * {@code find} devuelva {@code null}, y un {@code persist} pendiente, que
     * devuelva la fila.
     */
    private boolean tieneElRol(Usuario usuario, Rol rol) {
        return entityManager.find(UsuarioRol.class,
                new UsuarioRolId(usuario.getIdUsuario(), rol.getIdRol())) != null;
    }

    private static String rutaDeQuitar(Usuario usuario, Rol rol) {
        return "/api/usuario-roles/usuario/" + usuario.getIdUsuario() + "/rol/" + rol.getIdRol();
    }

    /**
     * El cuerpo de {@code PUT /api/usuarios/{id}}: nombres y apellidos, los dos
     * unicos campos que quedaron en {@code UsuarioUpdateRequest}. El correo y
     * el nombre de usuario salieron de ahi --son autoservicio del dueño-- y por
     * eso no estan aqui: si volvieran al DTO, este metodo seria el sitio donde
     * se notaria.
     */
    private static String cuerpoDeEdicion(String nombres, String apellidos) {
        return """
                { "nombres": "%s", "apellidos": "%s" }
                """.formatted(nombres, apellidos);
    }

    private static String cuerpoDeAsignacion(Usuario usuario, Rol rol) {
        return """
                { "idUsuario": %d, "idRol": %d }
                """.formatted(usuario.getIdUsuario(), rol.getIdRol());
    }

    /** Token cuyo {@code sub} es el id del usuario sembrado. */
    private String tokenDe(Usuario usuario, String... permisos) {
        UsuarioResponse respuesta = UsuarioResponse.builder()
                .id(usuario.getIdUsuario())
                .nombres(usuario.getNombres())
                .apellidos(usuario.getApellidos())
                .correo(usuario.getCorreo())
                .nombreUsuario(usuario.getNombreUsuario())
                .activo(true)
                .build();

        // El rol del claim no influye en nada: la autorizacion de estos
        // endpoints es por permiso (hasAuthority) y las reglas de SEG-H09
        // preguntan a la base, no al token.
        return "Bearer " + jwtService.emitir(respuesta, List.of(ADMINISTRADOR), List.of(permisos))
                .token();
    }
}
