-- =====================================================================
-- V23 - Un cambio de imagen refresca la fecha de la especie
-- =====================================================================
-- Segunda mitad de la observación: cualquier cambio de imagen tiene que
-- refrescar la fecha de última actualización de la especie dueña. Hoy una
-- imagen se añade o se da de baja y la ficha sigue diciendo que no se ha
-- tocado desde hace semanas.
--
-- ---------------------------------------------------------------------
-- Por qué por trigger y no en Java
-- ---------------------------------------------------------------------
-- `especie.fecha_actualizacion` ya la escribe la base y solo la base: el
-- trigger `trg_especie_fecha_actualizacion` (BEFORE UPDATE) la reescribe en
-- cada modificación y la entidad la relee con @Generated. Ningún método de
-- Java la asigna, y es correcto que siga así: si el refresco se hiciera en el
-- servicio de imágenes, la fecha tendría dos dueños y bastaría una ruta nueva
-- que olvidara llamarlo para que volviera a quedarse vieja.
--
-- Hacerlo aquí además mantiene el cambio dentro del módulo de multimedia: no
-- hay que tocar nada del Java del módulo de especie.
--
-- ---------------------------------------------------------------------
-- Cómo se "toca" la fila
-- ---------------------------------------------------------------------
-- Con un UPDATE que no cambia ningún valor:
--
--     UPDATE especie SET fecha_actualizacion = fecha_actualizacion WHERE ...
--
-- Parece un no-op y es justamente lo que se quiere: el UPDATE existe solo
-- para que se dispare el BEFORE UPDATE de `especie`, que es quien pone
-- CURRENT_TIMESTAMP. Así la fecha la sigue escribiendo un único sitio y este
-- trigger no tiene que saber nada de cómo se calcula.
--
-- Lo que NO hace: no toca `estado_publicacion` ni `activa` ni ninguna otra
-- columna, así que una ficha PUBLICADA sigue PUBLICADA y sigue en el catálogo
-- después de cambiarle las imágenes. Refrescar la fecha no es editar la ficha
-- y no pasa por `invalidarRevisionPrevia`, que vive en Java y no se entera.
--
-- ---------------------------------------------------------------------
-- Por qué AFTER y por qué FOR EACH ROW
-- ---------------------------------------------------------------------
-- AFTER: el toque solo tiene sentido si la fila de la imagen de verdad quedó
-- escrita. En un BEFORE, un CHECK que falle después (ck_imagen_fuente_unica,
-- por ejemplo) abortaría la sentencia y el UPDATE de la especie se iría con
-- ella --correcto por transacción, pero el orden al revés no aporta nada--.
--
-- FOR EACH ROW: hace falta el id de la especie de cada fila, que un trigger
-- de sentencia no tiene. Son uno o dos UPDATE por operación de imagen, no un
-- volumen que preocupe.
--
-- ---------------------------------------------------------------------
-- NEW y OLD
-- ---------------------------------------------------------------------
-- En DELETE no hay NEW y en INSERT no hay OLD, así que leer el id a ciegas
-- revienta con "record NEW is not assigned yet". Se elige según TG_OP.
--
-- En un UPDATE se tocan las DOS especies cuando el id cambia. Hoy no puede
-- pasar --`id_especie` está mapeada como updatable = false y ninguna ruta la
-- mueve-- pero si algún día se permitiera, la ficha que pierde la imagen
-- también cambió y también tiene que enterarse. El IS DISTINCT FROM evita el
-- UPDATE repetido en el caso normal, donde las dos son la misma.
--
-- ---------------------------------------------------------------------
-- No hay recursión
-- ---------------------------------------------------------------------
-- Este trigger está en `imagen_especie` y escribe en `especie`; el de
-- `especie` está en `especie` y no escribe en ninguna otra tabla. El UPDATE
-- de aquí dispara aquél, aquél pone la fecha y ahí termina la cadena.
--
-- ---------------------------------------------------------------------
-- La imagen cuelga de la especie directamente
-- ---------------------------------------------------------------------
-- `imagen_especie.id_especie` es NOT NULL y está presente incluso cuando la
-- imagen también apunta a una parte comestible (la FK compuesta
-- fk_imagen_parte_especie garantiza que la parte sea de esa misma especie).
-- Así que no hay que cruzar por `especie_parte_comestible` para saber a quién
-- tocar: el id está en la propia fila.
--
-- ---------------------------------------------------------------------
-- Alcance
-- ---------------------------------------------------------------------
-- Solo imágenes de especie. `imagen_preparacion` cuelga de
-- `preparacion_consumo`, no de la especie, y los videos no entran en este
-- cambio: siguen siendo enlaces y nadie pidió que refresquen la fecha.


CREATE OR REPLACE FUNCTION tocar_especie_de_imagen()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    id_nueva  bigint;
    id_previa bigint;
BEGIN
    -- NEW no existe en DELETE; OLD no existe en INSERT.
    IF TG_OP = 'DELETE' THEN
        id_nueva  := NULL;
        id_previa := OLD.id_especie;
    ELSIF TG_OP = 'INSERT' THEN
        id_nueva  := NEW.id_especie;
        id_previa := NULL;
    ELSE
        id_nueva  := NEW.id_especie;
        id_previa := OLD.id_especie;
    END IF;

    -- El UPDATE es intencionadamente un no-op: existe para disparar el
    -- BEFORE UPDATE de especie, que es quien escribe la fecha.
    IF id_nueva IS NOT NULL THEN
        UPDATE especie
        SET fecha_actualizacion = fecha_actualizacion
        WHERE id_especie = id_nueva;
    END IF;

    -- Solo si la imagen cambió de especie; en el caso normal son la misma y
    -- este UPDATE no se ejecuta.
    IF id_previa IS NOT NULL AND id_previa IS DISTINCT FROM id_nueva THEN
        UPDATE especie
        SET fecha_actualizacion = fecha_actualizacion
        WHERE id_especie = id_previa;
    END IF;

    -- Un trigger AFTER ignora el valor devuelto, pero devolver el registro
    -- que corresponde deja la función reutilizable si algún día se usa en un
    -- BEFORE.
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

COMMENT ON FUNCTION tocar_especie_de_imagen() IS
    'Actualiza la especie dueña de una imagen para que su BEFORE UPDATE refresque fecha_actualizacion.';

-- Idempotente: si la migración se repitiera sobre una base que ya lo tiene,
-- CREATE TRIGGER fallaría. DROP IF EXISTS la deja repetible.
DROP TRIGGER IF EXISTS trg_imagen_especie_toca_especie ON imagen_especie;

CREATE TRIGGER trg_imagen_especie_toca_especie
    AFTER INSERT OR UPDATE OR DELETE ON imagen_especie
    FOR EACH ROW
    EXECUTE FUNCTION tocar_especie_de_imagen();
