package sv.edu.ues.fmp.flora.service;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.entity.Rol;
import sv.edu.ues.fmp.flora.entity.Usuario;
import sv.edu.ues.fmp.flora.exception.EstadoInvalidoException;
import sv.edu.ues.fmp.flora.exception.OperacionNoPermitidaException;
import sv.edu.ues.fmp.flora.repository.UsuarioRepository;
import sv.edu.ues.fmp.flora.repository.UsuarioRolRepository;

/**
 * Las siete reglas que protegen el acceso de administracion (SEG-H09).
 *
 * <h2>Que agujero cierra</h2>
 * El permiso {@code USUARIO_GESTIONAR} abre todo el modulo de seguridad, y eso
 * dejaba tres cosas posibles para cualquiera que lo tuviera:
 * <ul>
 *   <li><strong>Auto-promocion.</strong> Asignarse a si mismo el rol
 *       ADMINISTRADOR con un {@code POST /api/usuario-roles}. Un usuario de
 *       gestion se volvia administrador sin que nadie lo autorizara, que es la
 *       misma escalacion vertical que se cerro a nivel de endpoint, solo que
 *       un paso mas adentro.</li>
 *   <li><strong>Golpe entre administradores.</strong> Quitarle el rol o dar de
 *       baja a otro administrador --incluido el unico que quedara-- sin ser
 *       nadie en particular.</li>
 *   <li><strong>Quedarse sin administradores.</strong> Nada impedia dejar el
 *       sistema con cero administradores activos; a partir de ahi la
 *       administracion solo se recupera por SQL.</li>
 * </ul>
 * Se sostiene en {@code usuario.es_superadmin}, la marca que crea {@code V19} y
 * que ninguna ruta de la API escribe: el superadministrador se designa por
 * migracion.
 *
 * <h2>Las reglas</h2>
 * <table border="1">
 *   <caption>Datos: <em>quien llama</em> es el usuario del token y
 *   <em>objetivo</em> el usuario afectado</caption>
 *   <tr><th>Regla</th><th>Dice</th><th>Respuesta</th></tr>
 *   <tr><td>R1 Autoproteccion</td>
 *       <td>Nadie se desactiva a si mismo ni se quita su propio
 *           ADMINISTRADOR.</td><td>403</td></tr>
 *   <tr><td>R2 Solo el superadmin asigna</td>
 *       <td>Solo el superadministrador ASIGNA ADMINISTRADOR. Es lo que cierra
 *           la auto-promocion: con {@code USUARIO_GESTIONAR} ya no alcanza
 *           para volverse admin, ni a si mismo ni a un comparsa.</td>
 *       <td>403</td></tr>
 *   <tr><td>R3 Solo el superadmin quita</td>
 *       <td>Solo el superadministrador QUITA ADMINISTRADOR o desactiva a un
 *           administrador; un admin normal no puede con otro admin.</td>
 *       <td>403</td></tr>
 *   <tr><td>R4 Superadmin protegido</td>
 *       <td>A el nadie le quita el rol ni lo desactiva.</td><td>403</td></tr>
 *   <tr><td>R5 Nunca cero admins</td>
 *       <td>Ninguna operacion deja el sistema con cero administradores
 *           activos.</td><td>409</td></tr>
 *   <tr><td>R6 Superadmin solo se edita a si mismo</td>
 *       <td>A los datos del superadministrador no los cambia nadie mas que el.
 *           Es el lado de la EDICION de lo que R4 ya decia de la baja y del
 *           rol.</td>
 *       <td>403</td></tr>
 *   <tr><td>R7a Roles de sistema: solo el superadmin</td>
 *       <td>Cualquier mutacion de un rol con {@code es_sistema = true}
 *           --editar, renombrar, dar de baja, reactivar, agregar o quitar sus
 *           permisos-- la hace solo el superadministrador.</td>
 *       <td>403</td></tr>
 *   <tr><td>R7b Roles de sistema: nombre y fila intocables</td>
 *       <td>Renombrar o dar de baja un rol de sistema no lo puede hacer
 *           NADIE, el superadministrador incluido.</td>
 *       <td>409</td></tr>
 * </table>
 *
 * <h2>R7: por que el flag y no el nombre</h2>
 * R2, R3 y R5 preguntan si un rol <em>se llama</em> ADMINISTRADOR
 * ({@link #esElRolDeAdministrador(Rol)} compara por nombre, y
 * {@code UsuarioRolRepository} cuenta por nombre). El nombre de un rol es un
 * dato editable: {@code PUT /api/roles/{id}} escribe {@code rol.nombre} y no
 * pasaba por ninguna regla. Con {@code USUARIO_GESTIONAR} alcanzaba para
 * renombrar ADMINISTRADOR y, a partir de ahi, R2 dejaba de impedir la
 * auto-promocion, R3 dejaba de proteger a los demas administradores y el
 * invariante de R5 se cumplia trivialmente porque la cuenta de
 * administradores daba cero. Por {@code /api/rol-permisos} habia una segunda
 * via con el mismo efecto: vaciar de permisos el rol degrada a todos los
 * administradores --superadmin incluido-- de una sola peticion.
 * <p>
 * Ninguna de esas vias tocaba al <em>usuario</em> superadministrador, asi que
 * R4 y R6 seguian cumpliendose y la cuenta no se podia desactivar ni editar:
 * quedaba marcada como superadmin y sin ningun poder. Lo que se perdia no era
 * la cuenta, era la proteccion. R7 se ancla en {@code rol.es_sistema}
 * ({@code V20}), que ninguna ruta de la API escribe, igual que R6 se ancla en
 * {@code usuario.es_superadmin}.
 *
 * <h2>R7: por que dos caras y dos codigos</h2>
 * Las dos mitades responden preguntas distintas y por eso no comparten
 * codigo:
 * <ul>
 *   <li><strong>R7a es de autoridad</strong>: quien llama no puede hacer esto.
 *       403, como R2, R3, R4 y R6.</li>
 *   <li><strong>R7b es estructural</strong>: esto no se puede hacer, y no por
 *       falta de autoridad --el superadministrador la tiene-- sino porque
 *       choca con un recurso protegido. 409, que es el codigo con el que este
 *       backend contesta los conflictos, y el mismo que ya devolvia R5.</li>
 * </ul>
 * Un 403 al superadministrador seria mentira: le diria que le faltan permisos
 * cuando lo que pasa es que la operacion no existe para ese rol.
 *
 * <h2>R7 frente a R5: el orden y por que</h2>
 * En una baja de rol, R7 se evalua ANTES que R5, siguiendo el criterio de la
 * seccion siguiente: las prohibiciones absolutas van primero y no tiene
 * sentido contar administradores en una operacion que de todas formas no
 * estaba permitida. El efecto es que
 * {@link #verificarDesactivacionDeRol(Rol)} queda inalcanzable para el rol
 * ADMINISTRADOR --un rol de sistema ya no se da de baja nunca-- y se conserva
 * por dos motivos: sigue siendo el invariante de cualquier rol que algun dia
 * se llame ADMINISTRADOR sin llevar el flag (una base donde {@code V20} no
 * marcara lo que debia), y es la mitad de R5 que esta tarea no toca. La otra
 * mitad de R5, la de usuarios --no se puede desactivar al ultimo
 * administrador activo-- sigue plenamente vigente y con sus pruebas.
 *
 * <h2>Por que hizo falta R6</h2>
 * R3 y R4 cubrian la baja logica y el rol, las dos vias que quitan el acceso.
 * La via de <strong>edicion</strong> --{@code PUT /api/usuarios/{id}}-- no
 * pasaba por aqui, y por ahi un administrador normal cambiaba los datos del
 * superadministrador. No le podia poner {@code activo = false}
 * --{@code UsuarioUpdateRequest} no tiene ese campo-- pero si reescribirle el
 * {@code correo} y el {@code nombreUsuario}, que son sus credenciales de
 * identidad: con el correo apuntando a otro buzon, el
 * {@code POST /api/usuarios/recuperar-clave} --publico-- entregaba la cuenta
 * del superadministrador a quien hizo el cambio. No lo daba de baja: lo
 * suplantaba, y sin tocar ninguna de las cinco reglas.
 * <p>
 * Esa via se cerro por los dos lados a la vez, y por eso R6 no es la unica
 * pieza: {@code correo} y {@code nombreUsuario} salieron del DTO de edicion
 * --cambiarlos es autoservicio del dueño, con su clave, en
 * {@code PUT /api/usuarios/{id}/credenciales}-- de modo que la cadena de
 * recuperacion ya no se puede repuntar en <strong>ninguna</strong> cuenta.
 * R6 es la mitad que protege lo que queda editable (nombres y apellidos) y la
 * que deja la regla escrita donde se busca: el dia que el DTO de edicion vuelva
 * a crecer, el superadministrador ya esta cubierto.
 *
 * <h2>El orden importa</h2>
 * Primero R1 y R4, que son prohibiciones absolutas y valen incluso para el
 * superadministrador: asi el mensaje que recibe el superadmin que intenta
 * desactivarse es "no puede desactivar su propia cuenta" y no un 409 sobre el
 * numero de administradores. Luego R2 y R3, que son la pregunta de quien
 * llama. R5 al final: no tiene sentido contar administradores en una operacion
 * que de todas formas no estaba permitida, y es la unica que responde 409.
 *
 * <h2>Por que aqui y no en {@code @PreAuthorize}</h2>
 * Las reglas no se contestan con las autoridades del token: necesitan la marca
 * {@code es_superadmin} de la base, los roles vigentes del objetivo y, en R5,
 * una cuenta sobre toda la tabla. Method-security tendria que consultar la base
 * desde una expresion SpEL, y el resultado quedaria repartido entre tres
 * anotaciones en vez de leerse de corrido.
 * <p>
 * Y por que un componente y no codigo dentro de cada servicio: las mismas
 * reglas las aplican {@code UsuarioRolServiceImpl.asignar} y {@code .quitar}
 * --modulo usuario-rol--, {@code UsuarioServiceImpl.desactivar} y
 * {@code .actualizar} --modulo usuario--, {@code RolServiceImpl.actualizar},
 * {@code .desactivar} y {@code .reactivar} --modulo rol-- y
 * {@code RolPermisoServiceImpl.asignar} y {@code .quitar} --modulo
 * rol-permiso--. Nueve sitios. Repetirlas en
 * cada sitio es como se abren los huecos, y la cuarta via fue exactamente eso:
 * {@code DELETE /api/roles/{id}} sobre el rol ADMINISTRADOR lo desactivaba
 * para todos --dejando el sistema sin administradores-- sin pasar por ninguna
 * de estas reglas. Cerrarla fue una linea llamando aqui
 * ({@link #verificarDesactivacionDeRol(Rol)}) y no reescribir el criterio.
 *
 * <h2>Lo que no decide</h2>
 * Si quien llama tiene derecho a tocar el modulo de seguridad: eso ya lo
 * resolvio {@code USUARIO_GESTIONAR} en el {@code @PreAuthorize} del
 * controlador. Aqui se asume que el permiso esta y se pregunta otra cosa: si
 * <em>este</em> usuario puede hacerle <em>esto</em> a <em>ese</em> otro.
 */
@Component
@RequiredArgsConstructor
public class ProteccionDeAdministracion {

    /**
     * Nombre del rol de administracion en la tabla {@code rol}, tal como lo
     * dejo {@code V12}. Se compara por nombre y no por id porque
     * {@code id_rol} es IDENTITY y no vale lo mismo en dos bases; el nombre si,
     * que ademas es unico ({@code rol_nombre_key}).
     */
    public static final String ADMINISTRADOR = "ADMINISTRADOR";

    private final UsuarioAutenticado usuarioAutenticado;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;

    /**
     * Antes de asignar un rol a un usuario.
     * <p>
     * Solo mira el caso del rol ADMINISTRADOR; cualquier otro rol se asigna
     * como siempre, porque lo que esta tarea protege es el acceso de
     * administracion y no la gestion de roles en general.
     *
     * @param objetivo usuario que recibiria el rol
     * @param rol      rol que se le asignaria
     * @throws OperacionNoPermitidaException R2: quien llama no es el
     *                                       superadministrador
     */
    public void verificarAsignacionDeRol(Usuario objetivo, Rol rol) {
        if (!esElRolDeAdministrador(rol)) {
            return;
        }

        // R2. No hay excepcion para el caso "me lo asigno a mi mismo": ese es
        // justamente el que se esta cerrando, y cae aqui como cualquier otro.
        if (!quienLlamaEsSuperadmin()) {
            throw new OperacionNoPermitidaException(
                    "Solo el superadministrador puede asignar el rol " + ADMINISTRADOR
                            + "; el usuario " + objetivo.getNombreUsuario()
                            + " no puede ser promovido desde esta cuenta");
        }
    }

    /**
     * Antes de quitarle un rol a un usuario.
     *
     * @param objetivo usuario al que se le quitaria el rol
     * @param rol      rol que se le quitaria
     * @throws OperacionNoPermitidaException R1, R4 o R3
     * @throws EstadoInvalidoException       R5: seria el ultimo administrador
     *                                       activo
     */
    public void verificarQuitarRol(Usuario objetivo, Rol rol) {
        if (!esElRolDeAdministrador(rol)) {
            return;
        }

        Long idQuienLlama = usuarioAutenticado.id();

        // R1.
        if (idQuienLlama.equals(objetivo.getIdUsuario())) {
            throw new OperacionNoPermitidaException(
                    "No puede quitarse a si mismo el rol " + ADMINISTRADOR
                            + "; pidalo al superadministrador");
        }

        // R4.
        if (esSuperadmin(objetivo)) {
            throw new OperacionNoPermitidaException(
                    "El usuario " + objetivo.getNombreUsuario()
                            + " es el superadministrador: no se le puede quitar el rol "
                            + ADMINISTRADOR);
        }

        // R3.
        if (!esSuperadmin(quienLlama(idQuienLlama))) {
            throw new OperacionNoPermitidaException(
                    "Solo el superadministrador puede quitar el rol " + ADMINISTRADOR
                            + "; un administrador no puede hacerlo con otro");
        }

        // R5.
        verificarQueNoEsElUltimoAdministrador(objetivo,
                "No se puede quitar el rol " + ADMINISTRADOR + " al usuario "
                        + objetivo.getNombreUsuario()
                        + " porque es el ultimo administrador activo del sistema");
    }

    /**
     * Antes de dar de baja logica a un usuario.
     * <p>
     * R1 se aplica a cualquier usuario y no solo a los administradores: nadie
     * se desactiva a si mismo. Las demas reglas solo entran cuando el objetivo
     * es administrador; dar de baja a un investigador sigue siendo una
     * operacion normal de {@code USUARIO_GESTIONAR}.
     *
     * @param objetivo usuario que se desactivaria
     * @throws OperacionNoPermitidaException R1, R4 o R3
     * @throws EstadoInvalidoException       R5: seria el ultimo administrador
     *                                       activo
     */
    public void verificarDesactivacion(Usuario objetivo) {
        Long idQuienLlama = usuarioAutenticado.id();

        // R1. Vale para todos: un administrador que se desactiva a si mismo
        // deja ademas un rol ADMINISTRADOR asignado que no otorga nada, que es
        // el estado mas confuso en el que se puede quedar la tabla.
        if (idQuienLlama.equals(objetivo.getIdUsuario())) {
            throw new OperacionNoPermitidaException(
                    "No puede desactivar su propia cuenta; pidalo a otro administrador");
        }

        // R4.
        if (esSuperadmin(objetivo)) {
            throw new OperacionNoPermitidaException(
                    "El usuario " + objetivo.getNombreUsuario()
                            + " es el superadministrador: su cuenta no se puede desactivar");
        }

        // R3. Solo pesa si el objetivo es administrador; si no lo es, cualquier
        // gestor de usuarios puede darlo de baja como hasta ahora.
        if (esAdministrador(objetivo) && !esSuperadmin(quienLlama(idQuienLlama))) {
            throw new OperacionNoPermitidaException(
                    "Solo el superadministrador puede desactivar al administrador "
                            + objetivo.getNombreUsuario());
        }

        // R5.
        verificarQueNoEsElUltimoAdministrador(objetivo,
                "No se puede desactivar al usuario " + objetivo.getNombreUsuario()
                        + " porque es el ultimo administrador activo del sistema");
    }

    /**
     * Antes de cambiar los datos de un usuario.
     *
     * <h2>R6: a los datos del superadministrador solo los cambia el</h2>
     * Es la unica regla que aplica aqui, y solo mira si el objetivo lleva la
     * marca. Editar a cualquier otro usuario --incluido otro administrador--
     * sigue siendo una operacion normal de {@code USUARIO_GESTIONAR}: lo que
     * esta tarea protege es al superadministrador, no la edicion en general.
     *
     * <h2>Por que el superadmin si se edita a si mismo</h2>
     * R6 responde "quien" y no "que": la cuenta tiene que poder corregir su
     * propio nombre sin pedirselo a nadie, porque no hay nadie por encima a
     * quien pedirselo. Es la diferencia con R1, que prohibe en absoluto: ahi lo
     * peligroso era precisamente la operacion sobre uno mismo --quedarse sin
     * acceso-- y aqui es la operacion sobre otro.
     *
     * <h2>Lo que no decide</h2>
     * Ni el estado ({@code activo}) ni la marca ({@code es_superadmin}) ni los
     * roles: ninguno de los tres esta en {@code UsuarioUpdateRequest}, asi que
     * la via de edicion no los puede tocar ni para el superadministrador ni
     * para nadie. La baja la cubre {@link #verificarDesactivacion(Usuario)} y
     * el rol, {@link #verificarQuitarRol(Usuario, Rol)}.
     *
     * @param objetivo usuario cuyos datos se cambiarian
     * @throws OperacionNoPermitidaException R6: el objetivo es el
     *                                       superadministrador y quien llama
     *                                       no es el
     */
    public void verificarEdicion(Usuario objetivo) {
        if (!esSuperadmin(objetivo)) {
            return;
        }

        // R6. Se compara por id y no por la marca de quien llama: dos
        // superadmins no se editan entre si, igual que R4 no deja que uno
        // desactive al otro.
        if (usuarioAutenticado.id().equals(objetivo.getIdUsuario())) {
            return;
        }

        throw new OperacionNoPermitidaException(
                "El usuario " + objetivo.getNombreUsuario()
                        + " es el superadministrador: solo el puede cambiar sus datos");
    }

    /**
     * Antes de dar de baja logica un rol.
     *
     * <h2>Que via cierra</h2>
     * {@code DELETE /api/roles/{id}} sobre el rol ADMINISTRADOR. Era la sexta
     * via a "cero administradores" y la unica que no pasaba por ninguna de las
     * reglas de arriba: no le quita el rol a nadie --las filas de
     * {@code usuario_rol} siguen ahi-- pero lo deja {@code activo = false}, y
     * un rol dado de baja no otorga nada
     * ({@code obtenerPermisosEfectivosDeUsuario} lo salta, y
     * {@link #esAdministrador(Usuario)} tampoco lo cuenta). El efecto es el de
     * degradar a todos los administradores de una sola peticion.
     *
     * <h2>Solo el invariante, y por que</h2>
     * De las cinco reglas aqui aplica R5 y nada mas. R1 y R4 hablan de un
     * usuario objetivo y aqui no hay ninguno; R2 y R3 preguntan quien llama,
     * y responderlas tambien por esta via seria reservar al superadmin la
     * edicion del catalogo de roles --una decision distinta de la que esta
     * tarea cierra--. El agujero concreto es el estado al que lleva la
     * operacion, y eso es exactamente R5: 409, no 403.
     *
     * @param rol rol que se desactivaria
     * @throws EstadoInvalidoException R5: el sistema quedaria sin
     *                                 administradores activos
     */
    public void verificarDesactivacionDeRol(Rol rol) {
        if (!esElRolDeAdministrador(rol)) {
            return;
        }

        // Si el rol ya estaba dado de baja, volver a desactivarlo no cambia el
        // numero de administradores activos --que con el rol inactivo ya es
        // cero-- y el invariante no esta en juego. Es la misma salvedad que
        // verificarQueNoEsElUltimoAdministrador hace con el objetivo que ya no
        // cuenta: el 409 es para la operacion que causa el problema, no para
        // la que lo encuentra hecho.
        if (!Boolean.TRUE.equals(rol.getActivo())) {
            return;
        }

        // R5. No hay a quien excluir: la baja del rol se lo quita a todos a la
        // vez, asi que cualquier administrador activo de hoy es uno que se
        // quedaria sin el rol, y el sistema sin ninguno.
        long administradoresActivos =
                usuarioRolRepository.contarUsuariosActivosConRolActivo(ADMINISTRADOR);

        if (administradoresActivos > 0) {
            throw new EstadoInvalidoException(
                    "No se puede desactivar el rol " + ADMINISTRADOR
                            + " porque el sistema quedaria sin administradores activos");
        }
    }

    /**
     * R7a, antes de cualquier mutacion de un rol: solo el superadministrador
     * toca los roles de sistema.
     *
     * <h2>Donde se llama</h2>
     * En las cinco vias que mutan un rol o sus permisos:
     * {@code RolServiceImpl.actualizar}, {@code .desactivar} y
     * {@code .reactivar}, y {@code RolPermisoServiceImpl.asignar} y
     * {@code .quitar}. Que sean cinco es justamente el motivo de que la regla
     * viva aqui y no repetida en cada servicio: la cuarta via de SEG-H09 se
     * abrio exactamente asi.
     *
     * <h2>Lo que no decide</h2>
     * Si la mutacion concreta esta permitida siquiera para el
     * superadministrador: eso lo contestan
     * {@link #verificarRenombradoDeRol(Rol, String)} y
     * {@link #verificarBajaDeRol(Rol)}, que van despues. R7a solo responde
     * "quien".
     * <p>
     * Y no mira el nombre del rol: un rol de sistema lo es por el flag. Para
     * los roles personalizados --los que nacen por {@code POST /api/roles},
     * todos en {@code false}-- este metodo no hace nada y la gestion del
     * catalogo de roles sigue siendo de quien tenga {@code USUARIO_GESTIONAR}.
     *
     * @param rol rol que se mutaria
     * @throws OperacionNoPermitidaException R7a: es un rol de sistema y quien
     *                                       llama no es el superadministrador
     */
    public void verificarMutacionDeRol(Rol rol) {
        if (!esRolDeSistema(rol)) {
            return;
        }

        if (!quienLlamaEsSuperadmin()) {
            throw new OperacionNoPermitidaException(
                    "El rol " + rol.getNombre() + " es un rol de sistema: solo el "
                            + "superadministrador puede modificarlo");
        }
    }

    /**
     * R7b, el nombre: un rol de sistema no se renombra, ni siquiera el
     * superadministrador.
     *
     * <h2>Por que el nombre y no el resto</h2>
     * Porque el nombre es el dato del que colgaba la proteccion antes de
     * {@code V20}, y sigue colgando en parte: {@link #ADMINISTRADOR} se compara
     * por nombre en R2, R3 y R5, y {@code UsuarioRolRepository} cuenta por
     * nombre. Mientras eso siga asi, renombrar es la operacion que apaga las
     * reglas, y no hay ninguna razon de negocio para permitirla: el nombre de
     * un rol de sistema es parte de su identidad, no una etiqueta.
     * <p>
     * Que tampoco lo pueda el superadministrador no es desconfianza, es que la
     * operacion no deberia existir: un renombrado accidental --un PUT que
     * reenvia un nombre mal escrito junto con la descripcion nueva-- dejaria el
     * sistema sin administradores a efectos de R5 sin que nadie lo pidiera. La
     * descripcion y los permisos si los cambia, que es lo que de verdad se
     * administra.
     *
     * <h2>Que cuenta como renombrar</h2>
     * Cualquier valor distinto del actual, comparando exacto y no
     * {@code ignoreCase}: {@code rol_nombre_key} es un UNIQUE sensible a
     * mayusculas, asi que pasar de 'ADMINISTRADOR' a 'Administrador' tambien
     * cambia la fila --y rompe las comparaciones que usan {@code upper()}--.
     * Reenviar el mismo nombre no es un renombrado y deja pasar la peticion,
     * que es lo que permite cambiar solo la descripcion con el PUT completo que
     * expone la API.
     *
     * @param rol         rol que se editaria
     * @param nombreNuevo nombre que traeria el cuerpo de la peticion
     * @throws EstadoInvalidoException R7b: es un rol de sistema y el nombre
     *                                 cambia
     */
    public void verificarRenombradoDeRol(Rol rol, String nombreNuevo) {
        if (!esRolDeSistema(rol)) {
            return;
        }

        if (!Objects.equals(rol.getNombre(), nombreNuevo)) {
            throw new EstadoInvalidoException(
                    "No se puede renombrar un rol de sistema: " + rol.getNombre()
                            + " tiene que conservar su nombre. Si puede cambiar su "
                            + "descripcion y sus permisos");
        }
    }

    /**
     * R7b, la fila: un rol de sistema no se da de baja, ni siquiera el
     * superadministrador.
     *
     * <h2>Que via cierra</h2>
     * {@code DELETE /api/roles/{id}}, que en este proyecto es baja logica. Un
     * rol dado de baja no otorga nada --{@code obtenerPermisosEfectivosDeUsuario}
     * lo salta y {@link #esAdministrador(Usuario)} tampoco lo cuenta-- asi que
     * la baja del rol ADMINISTRADOR degrada a todos los administradores de una
     * peticion, y la del rol INVESTIGADOR deja sin trabajar a todo el personal
     * cientifico. R5 ya cubria el primer caso con un 409, pero solo cuando
     * quedaban administradores activos que contar: con cero, la baja se
     * permitia. Ahora no se permite nunca.
     *
     * <h2>Por que para nadie</h2>
     * Mismo motivo que el nombre: no es una operacion que el sistema necesite.
     * Un rol de sistema que no debe usarse se vacia de permisos --eso si lo
     * puede el superadministrador-- o se deja de asignar; darlo de baja no
     * aporta nada que no se consiga de otra forma y si deja el sistema en un
     * estado del que solo se sale por SQL.
     *
     * <h2>Va antes de R5</h2>
     * Y por eso {@link #verificarDesactivacionDeRol(Rol)} queda inalcanzable
     * para ADMINISTRADOR. Ver el encabezado de la clase.
     *
     * @param rol rol que se daria de baja
     * @throws EstadoInvalidoException R7b: es un rol de sistema
     */
    public void verificarBajaDeRol(Rol rol) {
        if (!esRolDeSistema(rol)) {
            return;
        }

        throw new EstadoInvalidoException(
                "El rol " + rol.getNombre() + " es un rol de sistema y no se puede "
                        + "eliminar. Si necesita retirarlo, quitele sus permisos o deje "
                        + "de asignarlo");
    }

    /**
     * Si el rol lleva la marca de {@code V20}.
     * <p>
     * Un {@code rol} nulo no es de sistema: el lado seguro aqui es el contrario
     * que en {@link #esSuperadmin(Usuario)}, porque una regla que bloqueara con
     * un nulo convertiria cualquier error de programacion en un 403 sobre todos
     * los roles. Los servicios siempre llaman con la entidad ya resuelta.
     */
    private static boolean esRolDeSistema(Rol rol) {
        return rol != null && Boolean.TRUE.equals(rol.getEsSistema());
    }

    /**
     * R5, el invariante de "nunca cero administradores".
     *
     * <h2>Por que se pregunta por los demas y no por el total</h2>
     * La cuenta excluye al objetivo, asi que responde directamente a "cuantos
     * administradores activos quedarian si esta operacion se ejecutara". Mirar
     * el total antes de la operacion no sirve: con dos administradores
     * cualquiera de las dos bajas es valida, con uno ninguna, y la diferencia
     * esta en si el afectado es uno de los que se cuentan.
     *
     * <h2>Por que solo se comprueba si el objetivo cuenta hoy</h2>
     * Si el objetivo ya esta inactivo, o si su rol ADMINISTRADOR no esta
     * vigente, la operacion no baja el numero de administradores activos y el
     * invariante no esta en juego. Sin esta condicion, una base que ya
     * estuviera en cero administradores activos --que es un problema, pero no
     * uno que cause esta operacion-- devolveria 409 a operaciones inocuas, como
     * limpiar el rol de una cuenta dada de baja hace meses.
     */
    private void verificarQueNoEsElUltimoAdministrador(Usuario objetivo, String mensaje) {
        if (!cuentaComoAdministradorActivo(objetivo)) {
            return;
        }

        long otros = usuarioRolRepository.contarUsuariosActivosConRolActivoExcepto(
                ADMINISTRADOR, objetivo.getIdUsuario());

        if (otros == 0) {
            throw new EstadoInvalidoException(mensaje);
        }
    }

    /** Si el rol que se esta asignando o quitando es el de administracion. */
    private static boolean esElRolDeAdministrador(Rol rol) {
        return rol != null && ADMINISTRADOR.equalsIgnoreCase(rol.getNombre());
    }

    /**
     * Si quien llama es el superadministrador.
     * <p>
     * La marca se lee de la base y no del token a proposito: el token no lleva
     * ningun claim de superadmin --no se agrego-- asi que no hay forma de
     * falsificarla firmando un token, y si algun dia la marca se moviera, la
     * decision siguiente ya usa el valor nuevo sin esperar a que caduque
     * ningun token.
     */
    private boolean quienLlamaEsSuperadmin() {
        return esSuperadmin(quienLlama(usuarioAutenticado.id()));
    }

    /**
     * El usuario del token, si existe en la base.
     * <p>
     * Puede no existir: el {@code sub} de un token vigente apunta a una fila
     * que pudo borrarse. En ese caso no es superadmin, que es el lado seguro.
     */
    private Optional<Usuario> quienLlama(Long idQuienLlama) {
        return usuarioRepository.findById(idQuienLlama);
    }

    private static boolean esSuperadmin(Optional<Usuario> usuario) {
        return usuario.map(ProteccionDeAdministracion::esSuperadmin).orElse(false);
    }

    private static boolean esSuperadmin(Usuario usuario) {
        return usuario != null && Boolean.TRUE.equals(usuario.getEsSuperadmin());
    }

    /** Si el usuario tiene asignado el rol ADMINISTRADOR y ese rol esta vigente. */
    private boolean esAdministrador(Usuario usuario) {
        return usuarioRolRepository.contarRolActivoDeUsuario(
                usuario.getIdUsuario(), ADMINISTRADOR) > 0;
    }

    /**
     * Si el usuario es uno de los administradores que el invariante de R5
     * cuenta: cuenta activa y rol ADMINISTRADOR vigente.
     */
    private boolean cuentaComoAdministradorActivo(Usuario usuario) {
        return Boolean.TRUE.equals(usuario.getActivo()) && esAdministrador(usuario);
    }
}
