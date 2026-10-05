-- V7__cargar_distritos_san_vicente_sur.sql
--
-- Carga los 6 distritos del municipio San Vicente Sur (departamento de San
-- Vicente), necesarios para desarrollar y probar ubicacion_especie.
--
-- Carga PARCIAL para desarrollo: el resto de los 262 distritos se agregará en
-- una migración posterior.
--
-- Fuente: Ley Especial para la Reestructuración Municipal (Decreto Legislativo
-- 762, vigente desde el 1 de mayo de 2024). Los distritos corresponden a los
-- municipios anteriores a la reforma.
--
-- Decisiones:
-- - id_municipio se resuelve por nombre (las PK son GENERATED ALWAYS AS
--   IDENTITY y no se depende de valores concretos).
-- - ON CONFLICT DO NOTHING sobre uk_distrito_municipio_nombre: la migración
--   futura con los 262 distritos podrá incluir estos 6 sin fallar por
--   duplicados.
-- - El bloque final verifica que queden exactamente 6 distritos. Si el
--   municipio no se encontró (nombre distinto), Flyway falla y revierte la
--   migración en lugar de insertar 0 filas en silencio.

INSERT INTO distrito (id_municipio, nombre)
SELECT m.id_municipio, d.nombre
FROM (VALUES
          ('San Vicente'),
          ('Guadalupe'),
          ('San Cayetano Istepeque'),
          ('Tecoluca'),
          ('Tepetitán'),
          ('Verapaz')
     ) AS d(nombre)
         JOIN municipio m ON m.nombre = 'San Vicente Sur'
    ON CONFLICT (id_municipio, nombre) DO NOTHING;

DO $$
DECLARE
total integer;
BEGIN
SELECT count(*) INTO total
FROM distrito d
         JOIN municipio m ON m.id_municipio = d.id_municipio
WHERE m.nombre = 'San Vicente Sur';

IF total <> 6 THEN
        RAISE EXCEPTION 'Se esperaban 6 distritos en San Vicente Sur y hay %', total;
END IF;
END $$;