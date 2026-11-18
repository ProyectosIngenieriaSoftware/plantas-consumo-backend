-- V20__rol_de_sistema.sql
--
-- SEG-H09, continuacion. Marca los roles que el sistema necesita para
-- funcionar, que es el dato que le faltaba a ProteccionDeAdministracion para
-- dejar de apoyarse en el NOMBRE de un rol.
--
--
-- POR QUE ES V20
-- ==============
-- V19 es la ultima version aplicada en florav1 (flyway_schema_history, version
-- 19, success = true) y tambien la ultima en disco, asi que V20 es el
-- siguiente numero libre. Ninguna migracion aplicada se modifica: reusar un
-- numero haria fallar el arranque por version repetida y editar un archivo ya
-- aplicado le rompe el checksum. Mismo criterio que V12, V17, V18 y V19.
--
--
-- QUE AGUJERO CIERRA
-- ==================
-- R2, R3 y R5 protegen el acceso de administracion preguntando si un rol se
-- llama 'ADMINISTRADOR' --ProteccionDeAdministracion.esElRolDeAdministrador
-- compara por nombre, y UsuarioRolRepository cuenta por nombre--. Pero el
-- nombre de un rol es un dato editable: PUT /api/roles/{id} escribe
-- rol.nombre y no pasaba por ninguna regla. Con USUARIO_GESTIONAR alcanzaba
-- para renombrar ADMINISTRADOR y, a partir de ahi:
--
--   * esElRolDeAdministrador() devolvia false en todas las reglas, asi que R2
--     dejaba de impedir la auto-promocion y R3 dejaba de proteger a los demas
--     administradores;
--   * contarUsuariosActivosConRolActivo('ADMINISTRADOR') devolvia 0, asi que
--     el invariante de R5 se cumplia trivialmente;
--   * y por /api/rol-permisos se le podian quitar los permisos al rol, lo que
--     degrada a todos los administradores --superadmin incluido-- de una sola
--     peticion.
--
-- Ninguna de esas tres vias tocaba al usuario superadministrador, asi que R4 y
-- R6 seguian intactas y la cuenta no se podia desactivar ni editar: quedaba
-- marcada como superadmin y sin ningun poder. Lo que se perdia no era la
-- cuenta, era la proteccion.
--
--
-- QUE HACE
-- ========
-- 1. Agrega rol.es_sistema BOOLEAN NOT NULL DEFAULT FALSE. Con el DEFAULT, los
--    roles que ya existen quedan en FALSE sin reescribirse y los que entren
--    despues --POST /api/roles-- nacen en FALSE sin nombrar la columna.
--
-- 2. Pone TRUE en ADMINISTRADOR e INVESTIGADOR, los dos roles del diseño que
--    dejo V12 tras retirar ADMIN, EDITOR y CONSULTOR.
--
-- 3. Verifica que quedaron exactamente esos dos en TRUE. Si no, falla y Flyway
--    revierte la migracion completa.
--
--
-- LAS DECISIONES
-- ==============
--
-- a. Se marca POR NOMBRE, aqui y una sola vez. Suena contradictorio con todo
--    lo anterior, pero es lo contrario: el nombre se usa para el censo
--    inicial --es el unico dato por el que hoy se pueden reconocer esos dos
--    roles-- y desde este momento la autoridad pasa a ser la columna. A partir
--    de aqui el nombre puede ser cualquier cosa y las reglas siguen valiendo;
--    de hecho R7 impide cambiarlo, justamente para que el censo siga siendo
--    legible por una persona.
--
-- b. Se resuelve por clave natural (rol.nombre) y no por id, igual que V12 y
--    V13: id_rol es IDENTITY y no vale lo mismo en dos bases. En florav1
--    ADMINISTRADOR es el id 1 e INVESTIGADOR el 2, pero eso no se escribe
--    aqui.
--
-- c. Se compara con upper(nombre) porque la unicidad del nombre de rol en el
--    esquema es rol_nombre_key, un UNIQUE de columna sensible a mayusculas:
--    'Administrador' y 'ADMINISTRADOR' SI podrian coexistir. La comparacion
--    insensible podria entonces casar dos filas, y por eso el bloque de
--    verificacion cuenta cuantas quedaron marcadas en vez de dar por hecho que
--    es una por nombre.
--
-- d. INVESTIGADOR tambien entra, no solo ADMINISTRADOR. No es un rol
--    privilegiado, pero es parte del diseño del sistema: es el rol con el que
--    trabajan los investigadores y el que V9 asigna a las cuentas de demo.
--    Que un gestor de usuarios pueda renombrarlo o vaciarlo de permisos deja
--    sin poder trabajar a todo el personal cientifico, y eso no es una
--    operacion de catalogo. Lo que esta tarea protege es la estructura de
--    autorizacion, no solo la cuspide.
--
-- e. No se agrega un indice ni un CHECK que impida poner es_sistema en TRUE a
--    mano. No hace falta: ninguna ruta de la API escribe la columna
--    --RolRequest no la lleva y RolMapper.toEntity y .updateEntity no la
--    tocan-- asi que marcar un rol como de sistema es una migracion y una
--    decision del equipo, igual que usuario.es_superadmin en V19.
--
-- f. El rol inactivo no se excluye. En florav1 ADMINISTRADOR e INVESTIGADOR
--    estan los dos activos, pero si alguno estuviera dado de baja seguiria
--    siendo un rol de sistema: el flag dice que es, no en que estado esta.
--    Reactivarlo es entonces cosa del superadministrador (R7), que es lo
--    correcto.


-- =====================================================================
-- 1. La columna
-- =====================================================================
-- IF NOT EXISTS para que el script sea idempotente: si una base de desarrollo
-- ya tuviera la columna agregada a mano, la migracion no revienta y sigue al
-- UPDATE, que es el que deja el estado correcto.

ALTER TABLE rol
    ADD COLUMN IF NOT EXISTS es_sistema BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN rol.es_sistema IS
    'Marca los roles que el sistema necesita para funcionar (ADMINISTRADOR e '
    'INVESTIGADOR). Solo el superadministrador puede mutarlos, y su nombre no '
    'se puede cambiar ni su fila dar de baja: las reglas de '
    'ProteccionDeAdministracion se anclan en esta columna y no en el nombre '
    'del rol (SEG-H09 R7). Se setea por migracion: ninguna ruta de la API '
    'escribe esta columna.';


-- =====================================================================
-- 2. Los dos roles del diseño
-- =====================================================================
-- El WHERE evita tocar las filas que ya estan como deben, de modo que un
-- segundo pase no escribe nada.

UPDATE rol
SET es_sistema = TRUE
WHERE upper(nombre) IN ('ADMINISTRADOR', 'INVESTIGADOR')
  AND es_sistema IS DISTINCT FROM TRUE;


-- =====================================================================
-- 3. Verificacion
-- =====================================================================
-- Que la migracion no se de por buena si la base no tiene los roles que esta
-- tarea supone. Sin esto, una base sin ADMINISTRADOR quedaria con la columna
-- creada, ningun rol marcado y R7 sin nada que proteger, en silencio.

DO $$
DECLARE
    v_marcados   integer;
    v_admin      integer;
    v_investiga  integer;
    v_sobrantes  text;
BEGIN
    SELECT count(*) INTO v_marcados
    FROM rol
    WHERE es_sistema;

    SELECT count(*) INTO v_admin
    FROM rol
    WHERE es_sistema AND upper(nombre) = 'ADMINISTRADOR';

    SELECT count(*) INTO v_investiga
    FROM rol
    WHERE es_sistema AND upper(nombre) = 'INVESTIGADOR';

    IF v_admin = 0 THEN
        RAISE EXCEPTION
            'V20: no hay ningun rol llamado ''ADMINISTRADOR'' que marcar como de '
            'sistema. Esta base no tiene el rol que dejo V12; creelo o marque a '
            'mano el que corresponda antes de volver a aplicar la migracion';
    END IF;

    IF v_investiga = 0 THEN
        RAISE EXCEPTION
            'V20: no hay ningun rol llamado ''INVESTIGADOR'' que marcar como de '
            'sistema. Esta base no tiene el rol que dejo V12; creelo o marque a '
            'mano el que corresponda antes de volver a aplicar la migracion';
    END IF;

    IF v_marcados <> v_admin + v_investiga THEN
        SELECT string_agg(nombre, ', ' ORDER BY nombre) INTO v_sobrantes
        FROM rol
        WHERE es_sistema
          AND upper(nombre) NOT IN ('ADMINISTRADOR', 'INVESTIGADOR');

        RAISE EXCEPTION
            'V20: quedaron roles marcados como de sistema que no son del diseño: %. '
            'Solo ADMINISTRADOR e INVESTIGADOR deben llevar es_sistema = TRUE',
            v_sobrantes;
    END IF;

    RAISE NOTICE
        'V20: rol.es_sistema creada y marcada en % rol(es) del diseño (ADMINISTRADOR, INVESTIGADOR)',
        v_marcados;
END $$;
