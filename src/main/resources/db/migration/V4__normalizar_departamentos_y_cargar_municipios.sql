-- V4__normalizar_departamentos_y_cargar_municipios.sql
--
-- Esta migración hace dos cosas, en este orden:
--   1. Corrige la ortografía de los nombres de departamento (tildes y eñe).
--   2. Carga los 44 municipios vigentes de El Salvador.
--
-- Sobre el punto 1: los datos del respaldo original venían sin tilde. Se
-- normalizan porque estos nombres se muestran en el catálogo público y para
-- mantener consistencia con los nombres de municipio y distrito.
--
-- Sobre el punto 2: la Ley Especial para la Reestructuración Municipal
-- (Decreto Legislativo 762, Diario Oficial N.º 110, Tomo 439, del 14 de junio
-- de 2023), vigente desde el 1 de mayo de 2024, redujo los municipios de 262
-- a 44. Los 262 municipios anteriores pasaron a ser DISTRITOS y se cargan en
-- una migración posterior.
--
-- El id_departamento se resuelve por nombre mediante JOIN en lugar de
-- escribirse literal, porque las PK son GENERATED ALWAYS AS IDENTITY y no se
-- debe depender de valores concretos.


-- =====================================================================
-- 1. Normalización de nombres de departamento
-- =====================================================================
-- Solo seis de los catorce requieren corrección; los demás ya están bien.

UPDATE departamento SET nombre = 'Ahuachapán' WHERE lower(nombre) = 'ahuachapan';
UPDATE departamento SET nombre = 'Cuscatlán'  WHERE lower(nombre) = 'cuscatlan';
UPDATE departamento SET nombre = 'Cabañas'    WHERE lower(nombre) = 'cabanas';
UPDATE departamento SET nombre = 'Usulután'   WHERE lower(nombre) = 'usulutan';
UPDATE departamento SET nombre = 'Morazán'    WHERE lower(nombre) = 'morazan';
UPDATE departamento SET nombre = 'La Unión'   WHERE lower(nombre) = 'la union';


-- =====================================================================
-- 2. Carga de los 44 municipios
-- =====================================================================
-- Los nombres de departamento van CON tilde porque a esta altura del script
-- ya fueron corregidos por los UPDATE anteriores.

INSERT INTO municipio (id_departamento, nombre)
SELECT d.id_departamento, m.nombre
FROM (VALUES
          -- Ahuachapán (3)
          ('Ahuachapán',   'Ahuachapán Norte'),
          ('Ahuachapán',   'Ahuachapán Centro'),
          ('Ahuachapán',   'Ahuachapán Sur'),

          -- Santa Ana (4)
          ('Santa Ana',    'Santa Ana Norte'),
          ('Santa Ana',    'Santa Ana Centro'),
          ('Santa Ana',    'Santa Ana Este'),
          ('Santa Ana',    'Santa Ana Oeste'),

          -- Sonsonate (4)
          ('Sonsonate',    'Sonsonate Norte'),
          ('Sonsonate',    'Sonsonate Centro'),
          ('Sonsonate',    'Sonsonate Este'),
          ('Sonsonate',    'Sonsonate Oeste'),

          -- Chalatenango (3)
          ('Chalatenango', 'Chalatenango Norte'),
          ('Chalatenango', 'Chalatenango Centro'),
          ('Chalatenango', 'Chalatenango Sur'),

          -- La Libertad (6)
          ('La Libertad',  'La Libertad Norte'),
          ('La Libertad',  'La Libertad Centro'),
          ('La Libertad',  'La Libertad Oeste'),
          ('La Libertad',  'La Libertad Este'),
          ('La Libertad',  'La Libertad Costa'),
          ('La Libertad',  'La Libertad Sur'),

          -- San Salvador (5)
          ('San Salvador', 'San Salvador Norte'),
          ('San Salvador', 'San Salvador Oeste'),
          ('San Salvador', 'San Salvador Este'),
          ('San Salvador', 'San Salvador Centro'),
          ('San Salvador', 'San Salvador Sur'),

          -- Cuscatlán (2)
          ('Cuscatlán',    'Cuscatlán Norte'),
          ('Cuscatlán',    'Cuscatlán Sur'),

          -- La Paz (3)
          ('La Paz',       'La Paz Oeste'),
          ('La Paz',       'La Paz Centro'),
          ('La Paz',       'La Paz Este'),

          -- Cabañas (2)
          ('Cabañas',      'Cabañas Este'),
          ('Cabañas',      'Cabañas Oeste'),

          -- San Vicente (2)
          ('San Vicente',  'San Vicente Norte'),
          ('San Vicente',  'San Vicente Sur'),

          -- Usulután (3)
          ('Usulután',     'Usulután Norte'),
          ('Usulután',     'Usulután Este'),
          ('Usulután',     'Usulután Oeste'),

          -- San Miguel (3)
          ('San Miguel',   'San Miguel Norte'),
          ('San Miguel',   'San Miguel Centro'),
          ('San Miguel',   'San Miguel Oeste'),

          -- Morazán (2)
          ('Morazán',      'Morazán Norte'),
          ('Morazán',      'Morazán Sur'),

          -- La Unión (2)
          ('La Unión',     'La Unión Norte'),
          ('La Unión',     'La Unión Sur')
     ) AS m(departamento, nombre)
         JOIN departamento d ON d.nombre = m.departamento;


-- =====================================================================
-- Verificaciones (ejecutar manualmente después de aplicar)
-- =====================================================================
-- Debe devolver 44:
--   SELECT count(*) FROM municipio;
--
-- Ningún departamento debe quedar con 0 municipios:
--   SELECT d.nombre, count(m.*) AS municipios
--   FROM departamento d
--   LEFT JOIN municipio m ON m.id_departamento = d.id_departamento
--   GROUP BY d.nombre
--   ORDER BY d.nombre;