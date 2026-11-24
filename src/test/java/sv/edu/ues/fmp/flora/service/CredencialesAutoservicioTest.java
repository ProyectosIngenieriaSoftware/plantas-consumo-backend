package sv.edu.ues.fmp.flora.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import sv.edu.ues.fmp.flora.dto.response.UsuarioResponse;
import sv.edu.ues.fmp.flora.entity.Usuario;

/**
 * Red de regresion del autoservicio de credenciales: el correo y el nombre de
 * usuario solo los cambia el propietario de la cuenta, con su clave.
 *
 * <h2>Que agujero cierra</h2>
 * {@code correo} y {@code nombreUsuario} estaban en
 * {@code UsuarioUpdateRequest}, el cuerpo de {@code PUT /api/usuarios/{id}}, y
 * ese endpoint solo pide {@code USUARIO_GESTIONAR}. O sea que un administrador
 * reescribia las credenciales de identidad de <strong>cualquier</strong>
 * cuenta. Y {@code POST /api/usuarios/recuperar-clave} es publico y resuelve la
 * cuenta por su correo, asi que la secuencia completa era:
 * <ol>
 *   <li>cambiarle el correo a la cuenta ajena, apuntandolo a un buzon propio;</li>
 *   <li>pedir la recuperacion de clave de ese correo --sin token, es
 *       publico--;</li>
 *   <li>recibir el enlace y restablecer la clave.</li>
 * </ol>
 * El resultado no era dar de baja la cuenta sino <strong>quedarse con
 * ella</strong>. Con la del superadministrador eso saltaba por encima de
 * SEG-H09 entero: R4 impedia desactivarlo y quitarle el rol, pero la edicion no
 * pasaba por ninguna regla.
 *
 * <h2>Por que el arreglo es para todas las cuentas y no solo para el superadmin</h2>
 * Proteger solo al superadministrador (eso es R6, en
 * {@link ProteccionDeAdministracionTest}) habria dejado la cadena viva para
 * todos los demas: el investigador al que un gestor de usuarios le repunta el
 * correo pierde su cuenta igual. La raiz no es a quien se le puede editar, es
 * que las credenciales de identidad no son un dato administrativo. Por eso
 * salieron del DTO de edicion --que se queda con nombres y apellidos-- y
 * tienen su propia ruta, {@code PUT /api/usuarios/{id}/credenciales}, con el
 * mismo guard de propiedad que el cambio de clave.
 *
 * <h2>Las dos barreras, y por que hacen falta las dos</h2>
 * El {@code @PreAuthorize} compara el {@code id} de la ruta con el {@code sub}
 * del token: corta el intento sobre una cuenta ajena antes de tocar la base, de
 * modo que un atacante no distingue "clave correcta sobre cuenta ajena" de
 * "clave incorrecta sobre cuenta ajena" --las dos dan 403--. La clave actual es
 * la segunda: el token prueba que hay una sesion abierta, la clave prueba que
 * esta la persona, y cambiar el correo es justamente la operacion que decide a
 * donde llegara el proximo enlace de recuperacion.
 *
 * <h2>Datos: siembra propia, sin residuo</h2>
 * Mismo patron que {@link ProteccionDeAdministracionTest}:
 * {@code @SpringBootTest} con {@code @Transactional} y todo sembrado aqui
 * dentro con el {@link EntityManager}; el rollback se lo lleva. La diferencia
 * es que estos usuarios si necesitan una clave <strong>usable</strong>, porque
 * el endpoint la comprueba con BCrypt: se hashea con el mismo
 * {@link PasswordEncoder} del contexto. Nada se busca entre los datos de las
 * migraciones.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CredencialesAutoservicioTest {

    private static final String USUARIO_GESTIONAR = "USUARIO_GESTIONAR";

    /** La clave de los usuarios sembrados, en claro. */
    private static final String CLAVE = "DeLaSiembra1#";

    private static final String CLAVE_EQUIVOCADA = "NoEsLaSuya9$";

    /** Para que los correos y nombres de usuario sembrados no choquen. */
    private static final AtomicLong SECUENCIA = new AtomicLong();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmailService emailService;

    /** El propietario de la cuenta que se va a cambiar. */
    private Usuario propietario;

    /** Un administrador con USUARIO_GESTIONAR: el que ya no puede. */
    private Usuario administrador;

    /** Para el 409: ya ocupa un correo y un nombre de usuario. */
    private Usuario otro;

    /** Con la marca es_superadmin, para el contrapeso de R6. */
    private Usuario superadmin;

    @BeforeEach
    void sembrar() {
        propietario = crearUsuario(false);
        administrador = crearUsuario(false);
        otro = crearUsuario(false);
        superadmin = crearUsuario(true);

        entityManager.flush();
    }

    // ---------- 1. El administrador ya no toca las credenciales de otro ----------

    /**
     * La prueba central: el PUT de edicion acepta la peticion --es su endpoint
     * y el administrador tiene el permiso-- pero el correo y el nombre de
     * usuario del objetivo <strong>no cambian</strong>, porque ya no estan en
     * {@code UsuarioUpdateRequest}.
     * <p>
     * Se afirma el 200 y no un 400: {@code FAIL_ON_UNKNOWN_PROPERTIES} esta
     * desactivado, asi que un cliente viejo que siga mandando los cuatro campos
     * no se rompe, solo deja de conseguir dos de ellos. Lo que esta prueba fija
     * es el efecto, que es lo que importaba.
     */
    @Test
    void administrador_intentaCambiarElCorreoDeOtro_noLoLogra() throws Exception {
        String correoAntes = propietario.getCorreo();
        String nombreUsuarioAntes = propietario.getNombreUsuario();

        mockMvc.perform(put("/api/usuarios/" + propietario.getIdUsuario())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador, USUARIO_GESTIONAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres": "Reescrito",
                                  "apellidos": "PorAdmin",
                                  "correo": "buzon.del.atacante@prueba.local",
                                  "nombreUsuario": "atacante.suplanta"
                                }
                                """))
                .andExpect(status().isOk())
                // La respuesta ya devuelve el correo de siempre: el cambio no
                // ocurrio, no es que ocurriera y no se reflejara.
                .andExpect(jsonPath("$.correo").value(correoAntes))
                .andExpect(jsonPath("$.nombreUsuario").value(nombreUsuarioAntes))
                // Y lo que si es editable, se edito: la prueba no pasa por que
                // el endpoint entero dejara de funcionar.
                .andExpect(jsonPath("$.nombres").value("Reescrito"));

        Usuario despues = recargar(propietario);
        assertEquals(correoAntes, despues.getCorreo(),
                "un administrador no debe poder reescribir el correo de otra cuenta");
        assertEquals(nombreUsuarioAntes, despues.getNombreUsuario(),
                "un administrador no debe poder reescribir el nombre de usuario de otra cuenta");
    }

    /**
     * La consecuencia que se buscaba, dicha sobre la cadena completa: con el
     * correo intacto, el {@code recuperar-clave} publico sigue apuntando al
     * buzon del propietario y no al de quien intento el cambio.
     * <p>
     * No se ejerce el envio --{@code EmailService} esta mockeado-- sino el dato
     * del que depende: el correo con el que la cuenta se resuelve.
     */
    @Test
    void trasElIntento_laCuentaSigueResolviendosePorElCorreoDelPropietario() throws Exception {
        mockMvc.perform(put("/api/usuarios/" + propietario.getIdUsuario())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador, USUARIO_GESTIONAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres": "Reescrito",
                                  "apellidos": "PorAdmin",
                                  "correo": "buzon.del.atacante@prueba.local"
                                }
                                """))
                .andExpect(status().isOk());

        assertEquals(0L, cuantosUsuariosConCorreo("buzon.del.atacante@prueba.local"),
                "ninguna cuenta debia quedar apuntando al buzon del atacante");
    }

    // ---------- 2. El propietario si puede, con su clave ----------

    @Test
    void propietario_cambiaSusPropiasCredenciales_conSuClave_devuelve200() throws Exception {
        long n = SECUENCIA.incrementAndGet();
        String correoNuevo = "credenciales.nuevo." + n + "@prueba.local";
        String nombreUsuarioNuevo = "credenciales.nuevo." + n;

        mockMvc.perform(put(ruta(propietario))
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(propietario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(CLAVE, correoNuevo, nombreUsuarioNuevo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(correoNuevo))
                .andExpect(jsonPath("$.nombreUsuario").value(nombreUsuarioNuevo));

        Usuario despues = recargar(propietario);
        assertEquals(correoNuevo, despues.getCorreo(), "el correo debia quedar cambiado");
        assertEquals(nombreUsuarioNuevo, despues.getNombreUsuario(),
                "el nombre de usuario debia quedar cambiado");
    }

    /**
     * El superadministrador no es una excepcion por abajo: R6 le prohibe a los
     * demas editarlo, no a el gestionar su propia cuenta.
     */
    @Test
    void superadmin_cambiaSusPropiasCredenciales_devuelve200() throws Exception {
        long n = SECUENCIA.incrementAndGet();
        String correoNuevo = "credenciales.super." + n + "@prueba.local";

        mockMvc.perform(put(ruta(superadmin))
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(superadmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(CLAVE, correoNuevo, "credenciales.super." + n)))
                .andExpect(status().isOk());

        assertEquals(correoNuevo, recargar(superadmin).getCorreo(),
                "el superadministrador debia poder cambiar su propio correo");
    }

    // ---------- 3. Sin la clave correcta, no ----------

    /**
     * La clave equivocada da 401 --{@code CredencialesInvalidasException}-- y no
     * 403: aqui el guard de propiedad ya se atraveso, lo que fallo es la
     * credencial. Es el mismo codigo y el mismo mensaje que
     * {@code PUT /api/usuarios/{id}/clave}.
     */
    @Test
    void propietario_conLaClaveEquivocada_devuelve401YNoCambiaNada() throws Exception {
        String correoAntes = propietario.getCorreo();

        mockMvc.perform(put(ruta(propietario))
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(propietario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(CLAVE_EQUIVOCADA, "otro.correo@prueba.local", "otro.nombre")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("La clave actual no es correcta"));

        assertEquals(correoAntes, recargar(propietario).getCorreo(),
                "sin la clave correcta el correo no debia cambiar");
    }

    /**
     * Y sin el campo de la clave es 400 de validacion, antes de llegar al
     * servicio: {@code @NotBlank}. No puede ser 403 --el 403 esta reservado
     * para el guard de propiedad, que esta peticion si atraviesa-- y tenia que
     * quedar escrito para que nadie lo "arregle" devolviendo otra cosa.
     */
    @Test
    void propietario_sinElCampoDeLaClave_devuelve400YNoCambiaNada() throws Exception {
        String correoAntes = propietario.getCorreo();

        mockMvc.perform(put(ruta(propietario))
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(propietario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correo": "sin.clave@prueba.local",
                                  "nombreUsuario": "sin.clave"
                                }
                                """))
                .andExpect(status().isBadRequest());

        assertEquals(correoAntes, recargar(propietario).getCorreo(),
                "sin clave el correo no debia cambiar");
    }

    // ---------- 4. Sobre una cuenta ajena, 403 e indistinguible ----------

    /**
     * Las dos tentativas contra una cuenta ajena --con la clave correcta del
     * objetivo y con una cualquiera-- tienen que ser indistinguibles desde
     * fuera. Si se diferenciaran, el endpoint serviria para comprobar claves de
     * otras cuentas. Mismo razonamiento y mismo matcher que la prueba hermana
     * de {@code /clave} en {@code AutenticacionJwtTest}.
     */
    @Test
    void sobreCuentaAjena_devuelve403YNoDistingueSiLaClaveEraCorrecta() throws Exception {
        String correoAntes = propietario.getCorreo();

        for (String clave : List.of(CLAVE, CLAVE_EQUIVOCADA)) {
            mockMvc.perform(put(ruta(propietario))
                            .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador, USUARIO_GESTIONAR))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo(clave, "intruso@prueba.local", "intruso.suplanta")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.mensaje")
                            .value("No tiene los permisos necesarios para realizar esta operación"));
        }

        assertEquals(correoAntes, recargar(propietario).getCorreo(),
                "el correo de la cuenta ajena no debia cambiar");
    }

    /**
     * Ni con {@code USUARIO_GESTIONAR}: este endpoint no es de permiso, es de
     * propiedad. Un administrador no tiene una via "administrativa" de cambiar
     * el correo de otro, que es precisamente lo que se retiro.
     */
    @Test
    void elPermisoDeGestionNoAbreLaRutaDeOtro() throws Exception {
        mockMvc.perform(put(ruta(superadmin))
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(administrador, USUARIO_GESTIONAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(CLAVE, "intruso.super@prueba.local", "intruso.super")))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinToken_devuelve401YNo403() throws Exception {
        mockMvc.perform(put(ruta(propietario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(CLAVE, "anonimo@prueba.local", "anonimo.sin.token")))
                .andExpect(status().isUnauthorized());
    }

    // ---------- 5. La unicidad sigue en pie ----------

    @Test
    void correoYaOcupado_devuelve409YNoCambiaNada() throws Exception {
        String correoAntes = propietario.getCorreo();

        mockMvc.perform(put(ruta(propietario))
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(propietario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(CLAVE, otro.getCorreo(), "libre." + SECUENCIA.incrementAndGet())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409));

        assertEquals(correoAntes, recargar(propietario).getCorreo(),
                "el correo no debia cambiar al chocar con otro");
    }

    @Test
    void nombreDeUsuarioYaOcupado_devuelve409() throws Exception {
        mockMvc.perform(put(ruta(propietario))
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(propietario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(CLAVE,
                                "libre." + SECUENCIA.incrementAndGet() + "@prueba.local",
                                otro.getNombreUsuario())))
                .andExpect(status().isConflict());
    }

    /**
     * Reenviar sus propios valores no es un choque consigo mismo: el filtro de
     * unicidad excluye al propio id. Sin el, la cuenta no podria cambiar solo
     * uno de los dos campos.
     */
    @Test
    void reenviarSusPropiosValores_devuelve200() throws Exception {
        mockMvc.perform(put(ruta(propietario))
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(propietario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(CLAVE, propietario.getCorreo(), propietario.getNombreUsuario())))
                .andExpect(status().isOk());
    }

    // ---------- Siembra y utilidades ----------

    private static String ruta(Usuario usuario) {
        return "/api/usuarios/" + usuario.getIdUsuario() + "/credenciales";
    }

    private static String cuerpo(String claveActual, String correo, String nombreUsuario) {
        return """
                {
                  "claveActual": "%s",
                  "correo": "%s",
                  "nombreUsuario": "%s"
                }
                """.formatted(claveActual, correo, nombreUsuario);
    }

    private Usuario crearUsuario(boolean esSuperadmin) {
        long n = SECUENCIA.incrementAndGet();

        Usuario usuario = Usuario.builder()
                .nombres("Credenciales")
                .apellidos("Prueba" + n)
                .correo("credenciales." + n + "@prueba.local")
                .nombreUsuario("credenciales." + n)
                // Clave usable de verdad, al contrario que en las demas clases:
                // este endpoint compara la clave actual con BCrypt.
                .claveHash(passwordEncoder.encode(CLAVE))
                .activo(true)
                .esSuperadmin(esSuperadmin)
                .build();

        entityManager.persist(usuario);
        return usuario;
    }

    private Usuario recargar(Usuario usuario) {
        return entityManager.find(Usuario.class, usuario.getIdUsuario());
    }

    private long cuantosUsuariosConCorreo(String correo) {
        return entityManager.createQuery(
                        "SELECT count(u) FROM Usuario u WHERE lower(u.correo) = lower(:correo)",
                        Long.class)
                .setParameter("correo", correo)
                .getSingleResult();
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

        return "Bearer " + jwtService.emitir(respuesta, List.of("ADMINISTRADOR"), List.of(permisos))
                .token();
    }
}
