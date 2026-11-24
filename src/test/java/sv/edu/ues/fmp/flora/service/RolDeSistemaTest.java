package sv.edu.ues.fmp.flora.service;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import sv.edu.ues.fmp.flora.entity.Permiso;
import sv.edu.ues.fmp.flora.entity.Rol;
import sv.edu.ues.fmp.flora.entity.RolPermiso;
import sv.edu.ues.fmp.flora.entity.Usuario;
import sv.edu.ues.fmp.flora.entity.enums.RolPermisoId;

/**
 * Red de regresion de SEG-H09 R7: los roles de sistema.
 *
 * <h2>Que agujero cierra</h2>
 * R2, R3 y R5 protegen el acceso de administracion preguntando si un rol
 * <em>se llama</em> ADMINISTRADOR --{@code esElRolDeAdministrador} compara por
 * nombre, y {@code UsuarioRolRepository} cuenta por nombre--. Pero el nombre de
 * un rol es un dato editable y las rutas de rol y de rol-permiso no tenian
 * ninguna guarda, asi que con {@code USUARIO_GESTIONAR} alcanzaba para:
 * <ul>
 *   <li><strong>renombrar</strong> el rol ADMINISTRADOR, con lo que
 *       {@code esElRolDeAdministrador} devolvia false en R2, R3 y R5 y el
 *       contador de administradores activos daba cero: las tres reglas
 *       quedaban satisfechas trivialmente y la auto-promocion volvia a ser
 *       posible;</li>
 *   <li><strong>vaciarlo de permisos</strong> por {@code /api/rol-permisos},
 *       que degrada a todos los administradores --superadmin incluido-- de una
 *       sola peticion y sin tocar ninguna fila de {@code usuario_rol};</li>
 *   <li><strong>darlo de baja</strong>, que tiene el mismo efecto que lo
 *       anterior (un rol inactivo no otorga nada).</li>
 * </ul>
 * Ninguna de esas vias tocaba al <em>usuario</em> superadministrador, asi que
 * R4 y R6 seguian cumpliendose: la cuenta no se podia desactivar ni editar y
 * quedaba marcada como superadmin y sin ningun poder. Lo que se perdia no era
 * la cuenta, era la proteccion.
 *
 * <h2>Las dos caras de R7, y por que dos codigos</h2>
 * <table border="1">
 *   <caption>Que responde cada operacion sobre un rol de sistema</caption>
 *   <tr><th>Operacion</th><th>Admin normal</th><th>Superadmin</th></tr>
 *   <tr><td>Cambiar descripcion</td><td>403 R7a</td><td>200</td></tr>
 *   <tr><td>Cambiar permisos (POST/DELETE rol-permisos)</td>
 *       <td>403 R7a</td><td>201 / 204</td></tr>
 *   <tr><td>Reactivar</td><td>403 R7a</td><td>200</td></tr>
 *   <tr><td>Renombrar</td><td>403 R7a</td><td><strong>409 R7b</strong></td></tr>
 *   <tr><td>Dar de baja</td><td>403 R7a</td><td><strong>409 R7b</strong></td></tr>
 * </table>
 * R7a es de autoridad --quien llama no puede-- y contesta 403, como R2, R3, R4
 * y R6. R7b es estructural --esto no se puede hacer, y no por falta de
 * autoridad-- y contesta 409, el codigo de conflicto del resto del backend y el
 * mismo que ya devolvia R5. Un 403 al superadministrador seria mentira: le
 * diria que le faltan permisos cuando lo que pasa es que la operacion no
 * existe para esa clase de rol.
 * <p>
 * El orden importa y se comprueba: R7a antes que R7b. Un administrador normal
 * que intenta renombrar recibe 403 y no 409, porque la primera pregunta es
 * quien llama.
 *
 * <h2>Los contrapesos</h2>
 * La mitad de las pruebas son el contrapeso: crear un rol, y editar, renombrar,
 * dar de baja, reactivar y cambiar los permisos de un rol
 * <strong>personalizado</strong> sigue funcionando para quien tenga
 * {@code USUARIO_GESTIONAR}. Sin ellas, un guard que bloqueara todos los roles
 * dejaria la clase en verde y la gestion del catalogo inservible.
 *
 * <h2>Datos: siembra propia, sin residuo</h2>
 * Mismo patron que {@link ProteccionDeAdministracionTest} y
 * {@code SiembraDeFichas}: {@code @SpringBootTest} con {@code @Transactional} y
 * todo sembrado aqui dentro con el {@link EntityManager}; el rollback se lo
 * lleva y el unico rastro es el avance de las secuencias de id, inevitable con
 * {@code GenerationType.IDENTITY}.
 * <p>
 * No se usa el rol ADMINISTRADOR de la base: esta clase siembra sus propios
 * roles --uno con {@code es_sistema = true} y uno sin-- para no depender de lo
 * que {@code V20} haya marcado en la base de quien corra las pruebas, y para
 * poder renombrarlos y darlos de baja sin tocar nada real. Los correos llevan
 * dominio con punto por el CHECK {@code ck_usuario_correo}
 * ({@code correo ~~ '%_@_%._%'}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RolDeSistemaTest {

    private static final String USUARIO_GESTIONAR = "USUARIO_GESTIONAR";

    /** Para que los nombres de rol, de usuario y de permiso no choquen. */
    private static final AtomicLong SECUENCIA = new AtomicLong();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private EmailService emailService;

    /** Lleva es_sistema = true: el objetivo de R7. */
    private Rol rolDeSistema;

    /** Nace en false, como cualquier rol creado por la API: el contrapeso. */
    private Rol rolPersonalizado;

    /** Un permiso suelto, para los POST de rol-permiso. */
    private Permiso permisoLibre;

    /** Un permiso ya asignado al rol de sistema, para los DELETE. */
    private Permiso permisoAsignado;

    private Usuario superadmin;

    /** Administrador normal: sin la marca. Sus tokens llevan el permiso. */
    private Usuario administrador;

    @BeforeEach
    void sembrar() {
        rolDeSistema = crearRol(true);
        rolPersonalizado = crearRol(false);

        permisoLibre = crearPermiso();
        permisoAsignado = crearPermiso();
        asignarPermiso(rolDeSistema, permisoAsignado);

        superadmin = crearUsuario(true);
        administrador = crearUsuario(false);

        entityManager.flush();
    }

    // ---------- R7a: editar un rol de sistema ----------

    @Test
    void administrador_intentaEditarUnRolDeSistema_devuelve403YNoCambiaNada() throws Exception {
        String descripcionAntes = rolDeSistema.getDescripcion();

        mockMvc.perform(put("/api/roles/" + rolDeSistema.getIdRol())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeRol(rolDeSistema.getNombre(), "Descripcion del intruso")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("Prohibido"))
                .andExpect(jsonPath("$.mensaje").value(containsString("rol de sistema")))
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertEquals(descripcionAntes, recargar(rolDeSistema).getDescripcion(),
                "la descripcion del rol de sistema no debia cambiar");
    }

    /** Lo que el superadministrador SI puede: la descripcion. */
    @Test
    void superadmin_cambiaLaDescripcionDeUnRolDeSistema_devuelve200() throws Exception {
        mockMvc.perform(put("/api/roles/" + rolDeSistema.getIdRol())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(superadmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeRol(rolDeSistema.getNombre(), "Descripcion corregida")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Descripcion corregida"))
                .andExpect(jsonPath("$.nombre").value(rolDeSistema.getNombre()))
                .andExpect(jsonPath("$.esSistema").value(true));

        assertEquals("Descripcion corregida", recargar(rolDeSistema).getDescripcion(),
                "el superadministrador debia poder corregir la descripcion");
    }

    // ---------- R7b: el nombre ----------

    /**
     * El bloqueo estructural: ni el superadministrador renombra un rol de
     * sistema. 409 y no 403, porque no le falta autoridad --acaba de cambiarle
     * la descripcion en la prueba de arriba-- sino que la operacion no existe.
     */
    @Test
    void superadmin_intentaRenombrarUnRolDeSistema_devuelve409YElNombreQuedaIntacto()
            throws Exception {

        String nombreAntes = rolDeSistema.getNombre();

        mockMvc.perform(put("/api/roles/" + rolDeSistema.getIdRol())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(superadmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeRol("RENOMBRADO_" + SECUENCIA.incrementAndGet(),
                                rolDeSistema.getDescripcion())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje").value(containsString("renombrar")));

        assertEquals(nombreAntes, recargar(rolDeSistema).getNombre(),
                "el nombre de un rol de sistema no debia cambiar");
    }

    /**
     * Y al administrador normal lo corta R7a antes de que R7b llegue a opinar:
     * 403, no 409. Es la prueba del ORDEN de las dos caras.
     */
    @Test
    void administrador_intentaRenombrarUnRolDeSistema_devuelve403YNo409() throws Exception {
        String nombreAntes = rolDeSistema.getNombre();

        mockMvc.perform(put("/api/roles/" + rolDeSistema.getIdRol())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeRol("RENOMBRADO_" + SECUENCIA.incrementAndGet(),
                                rolDeSistema.getDescripcion())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403));

        assertEquals(nombreAntes, recargar(rolDeSistema).getNombre(),
                "el nombre de un rol de sistema no debia cambiar");
    }

    /**
     * Cambiar solo las mayusculas tambien es renombrar: {@code rol_nombre_key}
     * es un UNIQUE sensible a mayusculas y las comparaciones de SEG-H09 usan
     * {@code upper()}, asi que 'Administrador' no es el mismo dato que
     * 'ADMINISTRADOR' aunque case con las mismas consultas. La comparacion de
     * R7b es exacta a proposito.
     */
    @Test
    void superadmin_intentaCambiarSoloLasMayusculasDelNombre_devuelve409() throws Exception {
        String nombreAntes = rolDeSistema.getNombre();

        mockMvc.perform(put("/api/roles/" + rolDeSistema.getIdRol())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(superadmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeRol(nombreAntes.toLowerCase(),
                                rolDeSistema.getDescripcion())))
                .andExpect(status().isConflict());

        assertEquals(nombreAntes, recargar(rolDeSistema).getNombre(),
                "ni el cambio de mayusculas debia pasar");
    }

    // ---------- R7b: la baja ----------

    @Test
    void superadmin_intentaDarDeBajaUnRolDeSistema_devuelve409() throws Exception {
        mockMvc.perform(delete("/api/roles/" + rolDeSistema.getIdRol())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(superadmin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("no se puede")));

        assertTrue(recargar(rolDeSistema).getActivo(),
                "un rol de sistema no debia quedar desactivado");
    }

    @Test
    void administrador_intentaDarDeBajaUnRolDeSistema_devuelve403() throws Exception {
        mockMvc.perform(delete("/api/roles/" + rolDeSistema.getIdRol())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador)))
                .andExpect(status().isForbidden());

        assertTrue(recargar(rolDeSistema).getActivo(),
                "un rol de sistema no debia quedar desactivado");
    }

    // ---------- R7a: reactivar ----------

    /**
     * Reactivar no es renombrar ni eliminar, asi que solo entra R7a: al
     * administrador normal se le corta y al superadministrador no. La ruta
     * existe y muta el rol, aunque sea en la direccion segura, y por eso
     * tambien lleva la regla.
     */
    @Test
    void reactivarUnRolDeSistema_adminNormal403_superadmin200() throws Exception {
        mockMvc.perform(patch("/api/roles/" + rolDeSistema.getIdRol() + "/activar")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/roles/" + rolDeSistema.getIdRol() + "/activar")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(superadmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
    }

    // ---------- R7a: los permisos del rol ----------

    @Test
    void administrador_intentaAgregarUnPermisoAUnRolDeSistema_devuelve403() throws Exception {
        mockMvc.perform(post("/api/rol-permisos")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeRolPermiso(rolDeSistema, permisoLibre)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value(containsString("rol de sistema")));

        assertFalse(tieneElPermiso(rolDeSistema, permisoLibre),
                "la fila de rol_permiso no debia crearse");
    }

    /**
     * La mitad peligrosa: vaciar de permisos el rol ADMINISTRADOR degrada a
     * todos los administradores de una peticion, sin tocar ninguna fila de
     * {@code usuario_rol}. Es la razon de que R7 se ancle en el rol y no en el
     * usuario.
     */
    @Test
    void administrador_intentaQuitarUnPermisoAUnRolDeSistema_devuelve403() throws Exception {
        mockMvc.perform(delete(rutaDeRolPermiso(rolDeSistema, permisoAsignado))
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador)))
                .andExpect(status().isForbidden());

        assertTrue(tieneElPermiso(rolDeSistema, permisoAsignado),
                "la fila de rol_permiso no debia borrarse");
    }

    /** Y lo que el superadministrador si puede: administrar esos permisos. */
    @Test
    void superadmin_agregaYQuitaPermisosDeUnRolDeSistema_funciona() throws Exception {
        mockMvc.perform(post("/api/rol-permisos")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(superadmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeRolPermiso(rolDeSistema, permisoLibre)))
                .andExpect(status().isCreated());

        assertTrue(tieneElPermiso(rolDeSistema, permisoLibre),
                "el superadministrador debia poder agregar el permiso");

        mockMvc.perform(delete(rutaDeRolPermiso(rolDeSistema, permisoAsignado))
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(superadmin)))
                .andExpect(status().isNoContent());

        assertFalse(tieneElPermiso(rolDeSistema, permisoAsignado),
                "el superadministrador debia poder quitar el permiso");
    }

    // ---------- La marca es de solo lectura ----------

    /**
     * {@code esSistema} sale en la respuesta --el frontend la necesita para
     * bloquear la fila protegida-- y no entra por ningun cuerpo.
     * <p>
     * Se comprueba por las dos vias que escriben un rol: la creacion, que tiene
     * que nacir en false aunque el cuerpo pida true, y la edicion, que no la
     * cambia. Ningun DTO de entrada la lleva, asi que Jackson la descarta en
     * silencio ({@code FAIL_ON_UNKNOWN_PROPERTIES} esta desactivado); si algun
     * dia alguien la agregara a {@code RolRequest}, esta prueba es la que se
     * pone roja.
     */
    @Test
    void laMarcaDeSistema_seLeeEnLaRespuestaYNoSeEscribePorElCuerpo() throws Exception {
        // 1. Se lee, y distingue los dos roles sembrados.
        mockMvc.perform(get("/api/roles/" + rolDeSistema.getIdRol())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esSistema").value(true));

        mockMvc.perform(get("/api/roles/" + rolPersonalizado.getIdRol())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esSistema").value(false));

        // 2. El POST crea el rol en false aunque el cuerpo pida true.
        String nombreNuevo = "QA_R7_NACE_EN_FALSE_" + SECUENCIA.incrementAndGet();

        mockMvc.perform(post("/api/roles")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "%s",
                                  "descripcion": "Aspirante a rol de sistema",
                                  "esSistema": true,
                                  "es_sistema": true,
                                  "activo": false
                                }
                                """.formatted(nombreNuevo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.esSistema").value(false))
                // De paso: activo tampoco entra por el cuerpo.
                .andExpect(jsonPath("$.activo").value(true));

        // 3. El PUT no la cambia: el rol personalizado sigue en false y, por
        // tanto, sigue siendo editable por un administrador normal.
        mockMvc.perform(put("/api/roles/" + rolPersonalizado.getIdRol())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "%s",
                                  "descripcion": "Intento de ascenso",
                                  "esSistema": true
                                }
                                """.formatted(rolPersonalizado.getNombre())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esSistema").value(false));

        assertFalse(recargar(rolPersonalizado).getEsSistema(),
                "la marca no debia poder escribirse por el cuerpo de la edicion");
    }

    // ---------- Contrapesos: el rol personalizado no se sobre-bloquea ----------

    /**
     * Las cinco operaciones sobre un rol que NO es de sistema, con un
     * administrador normal. Si R7 sobre-bloqueara, la gestion del catalogo de
     * roles quedaria reservada al superadministrador y esta prueba lo diria.
     */
    @Test
    void administrador_gestionaUnRolPersonalizado_sigueFuncionando() throws Exception {
        // Editar y renombrar: las dos cosas que en un rol de sistema estan
        // cerradas.
        String nombreRenombrado = "QA_R7_RENOMBRADO_" + SECUENCIA.incrementAndGet();

        mockMvc.perform(put("/api/roles/" + rolPersonalizado.getIdRol())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeRol(nombreRenombrado, "Descripcion nueva")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(nombreRenombrado));

        // Permisos.
        mockMvc.perform(post("/api/rol-permisos")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeRolPermiso(rolPersonalizado, permisoLibre)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete(rutaDeRolPermiso(rolPersonalizado, permisoLibre))
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador)))
                .andExpect(status().isNoContent());

        // Baja y reactivacion.
        mockMvc.perform(delete("/api/roles/" + rolPersonalizado.getIdRol())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador)))
                .andExpect(status().isNoContent());

        assertFalse(recargar(rolPersonalizado).getActivo(), "el rol debia quedar desactivado");

        mockMvc.perform(patch("/api/roles/" + rolPersonalizado.getIdRol() + "/activar")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador)))
                .andExpect(status().isOk());

        assertTrue(recargar(rolPersonalizado).getActivo(), "el rol debia quedar reactivado");
    }

    /**
     * Y crear roles sigue siendo de quien tenga el permiso, con la marca en
     * false. Es el punto 7 del alcance: lo que no es de sistema no cambia.
     */
    @Test
    void administrador_creaUnRolNuevo_naceEnFalseYEsSuyo() throws Exception {
        String nombreNuevo = "QA_R7_NUEVO_" + SECUENCIA.incrementAndGet();

        mockMvc.perform(post("/api/roles")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeRol(nombreNuevo, "Rol creado por la prueba")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.esSistema").value(false));

        Rol creado = entityManager.createQuery(
                        "SELECT r FROM Rol r WHERE r.nombre = :nombre", Rol.class)
                .setParameter("nombre", nombreNuevo)
                .getSingleResult();

        // Y como nacio en false, el mismo administrador lo puede renombrar.
        mockMvc.perform(put("/api/roles/" + creado.getIdRol())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeRol(nombreNuevo + "_BIS", "Renombrado sin problema")))
                .andExpect(status().isOk());
    }

    // ---------- Un id inexistente sigue siendo 404 ----------

    /**
     * Mismo criterio que R6 en el modulo de usuario: las reglas hablan de un
     * rol concreto, asi que se evaluan despues de encontrarlo. Si R7 se
     * aplicara antes, un id inventado daria 403 y serviria para saber que el
     * guard existe.
     */
    @Test
    void unIdDeRolInexistente_sigueDando404() throws Exception {
        mockMvc.perform(put("/api/roles/999999")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeRol("QA_R7_NO_EXISTE", "No existe")))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/roles/999999")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/rol-permisos/rol/999999/permiso/999999")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador)))
                .andExpect(status().isNotFound());
    }

    // ---------- Siembra y utilidades ----------

    private static String cuerpoDeRol(String nombre, String descripcion) {
        return """
                { "nombre": "%s", "descripcion": "%s" }
                """.formatted(nombre, descripcion);
    }

    private static String cuerpoDeRolPermiso(Rol rol, Permiso permiso) {
        return """
                { "idRol": %d, "idPermiso": %d }
                """.formatted(rol.getIdRol(), permiso.getIdPermiso());
    }

    private static String rutaDeRolPermiso(Rol rol, Permiso permiso) {
        return "/api/rol-permisos/rol/" + rol.getIdRol() + "/permiso/" + permiso.getIdPermiso();
    }

    private Rol crearRol(boolean esSistema) {
        return persistir(Rol.builder()
                .nombre("QA_R7_" + (esSistema ? "SISTEMA_" : "PERSONALIZADO_")
                        + SECUENCIA.incrementAndGet())
                .descripcion("Rol sembrado por las pruebas de SEG-H09 R7")
                .activo(true)
                .esSistema(esSistema)
                .build());
    }

    private Permiso crearPermiso() {
        long n = SECUENCIA.incrementAndGet();

        return persistir(Permiso.builder()
                // codigo es unico (80 caracteres) y nombre es NOT NULL.
                .codigo("QA_R7_PERMISO_" + n)
                .nombre("Permiso de prueba R7 " + n)
                .descripcion("Permiso sembrado por las pruebas de SEG-H09 R7")
                .activo(true)
                .build());
    }

    private void asignarPermiso(Rol rol, Permiso permiso) {
        persistir(RolPermiso.builder()
                .id(new RolPermisoId(rol.getIdRol(), permiso.getIdPermiso()))
                .rol(rol)
                .permiso(permiso)
                .build());
    }

    private Usuario crearUsuario(boolean esSuperadmin) {
        long n = SECUENCIA.incrementAndGet();

        return persistir(Usuario.builder()
                .nombres("R7")
                .apellidos(esSuperadmin ? "Superadmin" : "Administrador")
                // Dominio con punto por ck_usuario_correo (correo ~~ '%_@_%._%').
                .correo("qa.r7." + n + "@prueba.local")
                .nombreUsuario("qa.r7." + n)
                // No es una clave usable: estos usuarios no inician sesion, el
                // token se emite directamente con JwtService.
                .claveHash("$2a$10$pruebaR7SinClaveUtilizable0000000000000000000000000000")
                .activo(true)
                .esSuperadmin(esSuperadmin)
                .build());
    }

    private <T> T persistir(T entidad) {
        entityManager.persist(entidad);
        return entidad;
    }

    /**
     * El rol tal como lo dejo la operacion. Mismo criterio que en
     * {@link ProteccionDeAdministracionTest}: se mira la sesion en la que
     * corrio la peticion y no se fuerza ningun {@code flush}, porque una regla
     * que corta deja la transaccion compartida marcada para rollback.
     */
    private Rol recargar(Rol rol) {
        return entityManager.find(Rol.class, rol.getIdRol());
    }

    private boolean tieneElPermiso(Rol rol, Permiso permiso) {
        return entityManager.find(RolPermiso.class,
                new RolPermisoId(rol.getIdRol(), permiso.getIdPermiso())) != null;
    }

    /** Token cuyo {@code sub} es el id del usuario sembrado. */
    private String tokenDe(Usuario usuario) {
        UsuarioResponse respuesta = UsuarioResponse.builder()
                .id(usuario.getIdUsuario())
                .nombres(usuario.getNombres())
                .apellidos(usuario.getApellidos())
                .correo(usuario.getCorreo())
                .nombreUsuario(usuario.getNombreUsuario())
                .activo(true)
                .build();

        // USUARIO_GESTIONAR siempre: estos endpoints lo exigen a nivel de
        // clase, y sin el los 403 vendrian de method-security en vez de R7a y
        // las pruebas no medirian nada de SEG-H09.
        return "Bearer " + jwtService.emitir(respuesta, List.of("ADMINISTRADOR"),
                List.of(USUARIO_GESTIONAR)).token();
    }
}
