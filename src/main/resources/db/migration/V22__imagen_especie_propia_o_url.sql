-- =====================================================================
-- V22 - La imagen de una especie puede ser un archivo propio
-- =====================================================================
-- Observación de la revisión (Dr. Dagoberto / ing. Melba): hasta ahora una
-- imagen solo podía ser un enlace externo. Se pide poder SUBIR imágenes
-- propias de cada especie.
--
-- El binario vive en el sistema de archivos del servidor, en la carpeta que
-- configura `app.imagenes.directorio`. En la base queda únicamente la
-- referencia y los metadatos; aquí no se guarda ningún byte de imagen.
--
-- ---------------------------------------------------------------------
-- Por qué una columna de origen y no solo "la que no sea null"
-- ---------------------------------------------------------------------
-- Se podría deducir el origen de qué columna está llena, pero entonces la
-- regla quedaría implícita en dos columnas nulables y una fila con las dos
-- llenas --o con las dos vacías-- sería un dato sin interpretación posible.
-- Con `origen` explícito, el CHECK puede exigir que el origen declarado y la
-- fuente presente coincidan.
--
-- ---------------------------------------------------------------------
-- Qué pasa con las 18 filas que ya existen
-- ---------------------------------------------------------------------
-- Todas son enlaces externos, así que el backfill las pone en 'URL' antes de
-- que la columna sea NOT NULL. Ese orden importa: añadir la columna ya como
-- NOT NULL sin DEFAULT fallaría sobre una tabla con filas, y ponerle un
-- DEFAULT 'URL' permanente haría que una imagen nueva que olvide declarar su
-- origen se guardara como enlace sin que nadie lo notara. La columna se añade
-- nulable, se rellena y se cierra.
--
-- ---------------------------------------------------------------------
-- ruta_archivo guarda un NOMBRE, no una ruta
-- ---------------------------------------------------------------------
-- La carpeta sale de la configuración y puede cambiar de una máquina a otra
-- (o de desarrollo a producción) sin tocar ninguna fila. Guardar aquí la ruta
-- absoluta ataría los datos al equipo donde se subió el archivo.
--
-- Los 120 caracteres sobran para lo que el sistema genera (un UUID de 36 más
-- la extensión), y dejan margen si alguna vez cambia el formato del nombre.
--
-- Nada de esto toca `baseline-version=3`: esta migración es posterior a V21 y
-- Flyway la aplica sola al arrancar.


-- ---------------------------------------------------------------------
-- 1. Columnas nuevas, todas nulables de entrada
-- ---------------------------------------------------------------------
ALTER TABLE imagen_especie
    ADD COLUMN origen          varchar(6),
    ADD COLUMN ruta_archivo    varchar(120),
    ADD COLUMN nombre_original varchar(255),
    ADD COLUMN tipo_contenido  varchar(100),
    ADD COLUMN tamanio_bytes   bigint;

COMMENT ON COLUMN imagen_especie.origen IS
    'PROPIA (archivo en el servidor) o URL (enlace externo). Exactamente una fuente por fila.';
COMMENT ON COLUMN imagen_especie.ruta_archivo IS
    'Nombre del archivo generado por el sistema (UUID + extensión) dentro de app.imagenes.directorio. Solo en PROPIA.';
COMMENT ON COLUMN imagen_especie.nombre_original IS
    'Nombre que traía el archivo del cliente. Informativo: nunca se usa para construir rutas.';
COMMENT ON COLUMN imagen_especie.tipo_contenido IS
    'Tipo detectado por los magic bytes del archivo, no el declarado por el cliente. Es el Content-Type al servirlo.';
COMMENT ON COLUMN imagen_especie.tamanio_bytes IS
    'Tamaño del archivo en disco.';


-- ---------------------------------------------------------------------
-- 2. Backfill: lo que ya existía son enlaces externos
-- ---------------------------------------------------------------------
UPDATE imagen_especie
SET origen = 'URL'
WHERE origen IS NULL;


-- ---------------------------------------------------------------------
-- 3. Cerrar origen y abrir url_imagen
-- ---------------------------------------------------------------------
-- url_imagen deja de ser NOT NULL porque una imagen PROPIA no tiene URL. Lo
-- que impide que quede una fila sin ninguna fuente es el CHECK del punto 5,
-- no la nulabilidad de las columnas.
ALTER TABLE imagen_especie ALTER COLUMN origen     SET NOT NULL;
ALTER TABLE imagen_especie ALTER COLUMN url_imagen DROP NOT NULL;


-- ---------------------------------------------------------------------
-- 4. Los valores admitidos del origen
-- ---------------------------------------------------------------------
-- Mismo criterio que ck_imagen_tipo y ck_especie_estado: el enum de Java se
-- persiste como texto y la base es la que fija la lista. Añadir un valor al
-- enum sin tocar este CHECK hace que la fila la rechace PostgreSQL.
ALTER TABLE imagen_especie
    ADD CONSTRAINT ck_imagen_origen
    CHECK (origen IN ('PROPIA', 'URL'));


-- ---------------------------------------------------------------------
-- 5. Exactamente una fuente por fila
-- ---------------------------------------------------------------------
-- La invariante del cambio. Cubre los cuatro casos malos de un golpe:
--   - URL sin url_imagen            -> no hay nada que mostrar
--   - URL con ruta_archivo          -> apunta a un archivo que nadie subió
--   - PROPIA sin ruta_archivo       -> no hay nada que servir
--   - PROPIA con url_imagen         -> dos fuentes, ¿cuál manda?
ALTER TABLE imagen_especie
    ADD CONSTRAINT ck_imagen_fuente_unica
    CHECK (
        (origen = 'URL'    AND url_imagen   IS NOT NULL AND ruta_archivo IS NULL)
     OR (origen = 'PROPIA' AND ruta_archivo IS NOT NULL AND url_imagen   IS NULL)
    );


-- ---------------------------------------------------------------------
-- 6. Un archivo, una fila
-- ---------------------------------------------------------------------
-- El nombre lo genera un UUID, así que una colisión es improbable; el índice
-- está por lo que sí puede pasar: que un reintento o un error de código
-- inserte dos filas apuntando al mismo archivo, y que dar de baja una deje
-- a la otra sirviendo un binario que ya nadie cree que exista.
--
-- Los NULL son distintos entre sí en un UNIQUE de PostgreSQL (no se usa
-- NULLS NOT DISTINCT a propósito), así que las filas URL no se estorban.
CREATE UNIQUE INDEX uk_imagen_ruta_archivo
    ON imagen_especie (ruta_archivo);
