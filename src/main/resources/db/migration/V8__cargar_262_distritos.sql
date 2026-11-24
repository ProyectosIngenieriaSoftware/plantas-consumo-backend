-- V8__cargar_262_distritos.sql
--
-- Carga los 262 distritos municipales de El Salvador, agrupados en los 44
-- municipios creados por la Ley Especial para la Reestructuracion Municipal
-- (Decreto Legislativo 762, vigente desde el 1 de mayo de 2024).
--
-- Decisiones:
-- - id_municipio se resuelve por (departamento, nombre de municipio) mediante
--   JOIN, igual que en V4 y V7: las PK son GENERATED ALWAYS AS IDENTITY y no se
--   depende de valores concretos.
-- - ON CONFLICT (id_municipio, nombre) DO NOTHING sobre
--   uk_distrito_municipio_nombre: los 6 distritos de San Vicente Sur que ya
--   cargo V7 (solo para desarrollo) no se duplican. Esta migracion tambien
--   funciona en una base que no tenga los datos de V7.
-- - El mismo nombre de distrito puede repetirse en municipios distintos
--   (por ejemplo El Rosario o San Fernando); la unicidad es por municipio.
-- - El bloque final verifica que los 262 distritos existan y que cada municipio
--   tenga los suyos. Si algo no se encontro (nombre de municipio o departamento
--   distinto), Flyway falla y revierte la migracion en lugar de cargar datos
--   incompletos en silencio.
--
-- Fuente: texto de la Ley Especial para la Reestructuracion Municipal,
-- segun la distribucion publicada por Diario El Mundo el 27 de abril de 2024.

CREATE TEMP TABLE tmp_distrito_v8
(
    departamento varchar(100) NOT NULL,
    municipio    varchar(100) NOT NULL,
    distrito     varchar(100) NOT NULL
);

INSERT INTO tmp_distrito_v8 (departamento, municipio, distrito)
VALUES
    -- Ahuachapán Norte (4)
    ('Ahuachapán', 'Ahuachapán Norte', 'Atiquizaya'),
    ('Ahuachapán', 'Ahuachapán Norte', 'El Refugio'),
    ('Ahuachapán', 'Ahuachapán Norte', 'San Lorenzo'),
    ('Ahuachapán', 'Ahuachapán Norte', 'Turín'),
    -- Ahuachapán Centro (4)
    ('Ahuachapán', 'Ahuachapán Centro', 'Ahuachapán'),
    ('Ahuachapán', 'Ahuachapán Centro', 'Apaneca'),
    ('Ahuachapán', 'Ahuachapán Centro', 'Concepción de Ataco'),
    ('Ahuachapán', 'Ahuachapán Centro', 'Tacuba'),
    -- Ahuachapán Sur (4)
    ('Ahuachapán', 'Ahuachapán Sur', 'Guaymango'),
    ('Ahuachapán', 'Ahuachapán Sur', 'Jujutla'),
    ('Ahuachapán', 'Ahuachapán Sur', 'San Francisco Menéndez'),
    ('Ahuachapán', 'Ahuachapán Sur', 'San Pedro Puxtla'),
    -- San Salvador Norte (3)
    ('San Salvador', 'San Salvador Norte', 'Aguilares'),
    ('San Salvador', 'San Salvador Norte', 'El Paisnal'),
    ('San Salvador', 'San Salvador Norte', 'Guazapa'),
    -- San Salvador Oeste (2)
    ('San Salvador', 'San Salvador Oeste', 'Apopa'),
    ('San Salvador', 'San Salvador Oeste', 'Nejapa'),
    -- San Salvador Este (4)
    ('San Salvador', 'San Salvador Este', 'Ilopango'),
    ('San Salvador', 'San Salvador Este', 'San Martín'),
    ('San Salvador', 'San Salvador Este', 'Soyapango'),
    ('San Salvador', 'San Salvador Este', 'Tonacatepeque'),
    -- San Salvador Centro (5)
    ('San Salvador', 'San Salvador Centro', 'Ayutuxtepeque'),
    ('San Salvador', 'San Salvador Centro', 'Mejicanos'),
    ('San Salvador', 'San Salvador Centro', 'San Salvador'),
    ('San Salvador', 'San Salvador Centro', 'Cuscatancingo'),
    ('San Salvador', 'San Salvador Centro', 'Ciudad Delgado'),
    -- San Salvador Sur (5)
    ('San Salvador', 'San Salvador Sur', 'Panchimalco'),
    ('San Salvador', 'San Salvador Sur', 'Rosario de Mora'),
    ('San Salvador', 'San Salvador Sur', 'San Marcos'),
    ('San Salvador', 'San Salvador Sur', 'Santo Tomás'),
    ('San Salvador', 'San Salvador Sur', 'Santiago Texacuangos'),
    -- La Libertad Norte (3)
    ('La Libertad', 'La Libertad Norte', 'Quezaltepeque'),
    ('La Libertad', 'La Libertad Norte', 'San Matías'),
    ('La Libertad', 'La Libertad Norte', 'San Pablo Tacachico'),
    -- La Libertad Centro (2)
    ('La Libertad', 'La Libertad Centro', 'San Juan Opico'),
    ('La Libertad', 'La Libertad Centro', 'Ciudad Arce'),
    -- La Libertad Oeste (5)
    ('La Libertad', 'La Libertad Oeste', 'Colón'),
    ('La Libertad', 'La Libertad Oeste', 'Jayaque'),
    ('La Libertad', 'La Libertad Oeste', 'Sacacoyo'),
    ('La Libertad', 'La Libertad Oeste', 'Tepecoyo'),
    ('La Libertad', 'La Libertad Oeste', 'Talnique'),
    -- La Libertad Este (5)
    ('La Libertad', 'La Libertad Este', 'Antiguo Cuscatlán'),
    ('La Libertad', 'La Libertad Este', 'Huizúcar'),
    ('La Libertad', 'La Libertad Este', 'Nuevo Cuscatlán'),
    ('La Libertad', 'La Libertad Este', 'San José Villanueva'),
    ('La Libertad', 'La Libertad Este', 'Zaragoza'),
    -- La Libertad Costa (5)
    ('La Libertad', 'La Libertad Costa', 'Chiltiupán'),
    ('La Libertad', 'La Libertad Costa', 'Jicalapa'),
    ('La Libertad', 'La Libertad Costa', 'La Libertad'),
    ('La Libertad', 'La Libertad Costa', 'Tamanique'),
    ('La Libertad', 'La Libertad Costa', 'Teotepeque'),
    -- La Libertad Sur (2)
    ('La Libertad', 'La Libertad Sur', 'Comasagua'),
    ('La Libertad', 'La Libertad Sur', 'Santa Tecla'),
    -- Chalatenango Norte (3)
    ('Chalatenango', 'Chalatenango Norte', 'La Palma'),
    ('Chalatenango', 'Chalatenango Norte', 'Citalá'),
    ('Chalatenango', 'Chalatenango Norte', 'San Ignacio'),
    -- Chalatenango Centro (10)
    ('Chalatenango', 'Chalatenango Centro', 'Nueva Concepción'),
    ('Chalatenango', 'Chalatenango Centro', 'Tejutla'),
    ('Chalatenango', 'Chalatenango Centro', 'La Reina'),
    ('Chalatenango', 'Chalatenango Centro', 'Agua Caliente'),
    ('Chalatenango', 'Chalatenango Centro', 'Dulce Nombre de María'),
    ('Chalatenango', 'Chalatenango Centro', 'El Paraíso'),
    ('Chalatenango', 'Chalatenango Centro', 'San Fernando'),
    ('Chalatenango', 'Chalatenango Centro', 'San Francisco Morazán'),
    ('Chalatenango', 'Chalatenango Centro', 'San Rafael'),
    ('Chalatenango', 'Chalatenango Centro', 'Santa Rita'),
    -- Chalatenango Sur (20)
    ('Chalatenango', 'Chalatenango Sur', 'Chalatenango'),
    ('Chalatenango', 'Chalatenango Sur', 'Arcatao'),
    ('Chalatenango', 'Chalatenango Sur', 'Azacualpa'),
    ('Chalatenango', 'Chalatenango Sur', 'Comalapa'),
    ('Chalatenango', 'Chalatenango Sur', 'Concepción Quezaltepeque'),
    ('Chalatenango', 'Chalatenango Sur', 'El Carrizal'),
    ('Chalatenango', 'Chalatenango Sur', 'La Laguna'),
    ('Chalatenango', 'Chalatenango Sur', 'Las Vueltas'),
    ('Chalatenango', 'Chalatenango Sur', 'Nombre de Jesús'),
    ('Chalatenango', 'Chalatenango Sur', 'Nueva Trinidad'),
    ('Chalatenango', 'Chalatenango Sur', 'Ojos de Agua'),
    ('Chalatenango', 'Chalatenango Sur', 'Potonico'),
    ('Chalatenango', 'Chalatenango Sur', 'San Antonio de la Cruz'),
    ('Chalatenango', 'Chalatenango Sur', 'San Antonio Los Ranchos'),
    ('Chalatenango', 'Chalatenango Sur', 'San Francisco Lempa'),
    ('Chalatenango', 'Chalatenango Sur', 'San Isidro Labrador'),
    ('Chalatenango', 'Chalatenango Sur', 'San José Cancasque'),
    ('Chalatenango', 'Chalatenango Sur', 'San Miguel de Mercedes'),
    ('Chalatenango', 'Chalatenango Sur', 'San José Las Flores'),
    ('Chalatenango', 'Chalatenango Sur', 'San Luis del Carmen'),
    -- Cuscatlán Norte (5)
    ('Cuscatlán', 'Cuscatlán Norte', 'Suchitoto'),
    ('Cuscatlán', 'Cuscatlán Norte', 'San José Guayabal'),
    ('Cuscatlán', 'Cuscatlán Norte', 'Oratorio de Concepción'),
    ('Cuscatlán', 'Cuscatlán Norte', 'San Bartolomé Perulapía'),
    ('Cuscatlán', 'Cuscatlán Norte', 'San Pedro Perulapán'),
    -- Cuscatlán Sur (11)
    ('Cuscatlán', 'Cuscatlán Sur', 'Cojutepeque'),
    ('Cuscatlán', 'Cuscatlán Sur', 'San Rafael Cedros'),
    ('Cuscatlán', 'Cuscatlán Sur', 'Candelaria'),
    ('Cuscatlán', 'Cuscatlán Sur', 'Monte San Juan'),
    ('Cuscatlán', 'Cuscatlán Sur', 'El Carmen'),
    ('Cuscatlán', 'Cuscatlán Sur', 'San Cristóbal'),
    ('Cuscatlán', 'Cuscatlán Sur', 'Santa Cruz Michapa'),
    ('Cuscatlán', 'Cuscatlán Sur', 'San Ramón'),
    ('Cuscatlán', 'Cuscatlán Sur', 'El Rosario'),
    ('Cuscatlán', 'Cuscatlán Sur', 'Santa Cruz Analquito'),
    ('Cuscatlán', 'Cuscatlán Sur', 'Tenancingo'),
    -- Cabañas Este (5)
    ('Cabañas', 'Cabañas Este', 'Sensuntepeque'),
    ('Cabañas', 'Cabañas Este', 'Victoria'),
    ('Cabañas', 'Cabañas Este', 'Dolores'),
    ('Cabañas', 'Cabañas Este', 'Guacotecti'),
    ('Cabañas', 'Cabañas Este', 'San Isidro'),
    -- Cabañas Oeste (4)
    ('Cabañas', 'Cabañas Oeste', 'Ilobasco'),
    ('Cabañas', 'Cabañas Oeste', 'Tejutepeque'),
    ('Cabañas', 'Cabañas Oeste', 'Jutiapa'),
    ('Cabañas', 'Cabañas Oeste', 'Cinquera'),
    -- La Paz Oeste (7)
    ('La Paz', 'La Paz Oeste', 'Cuyultitán'),
    ('La Paz', 'La Paz Oeste', 'Olocuilta'),
    ('La Paz', 'La Paz Oeste', 'San Juan Talpa'),
    ('La Paz', 'La Paz Oeste', 'San Luis Talpa'),
    ('La Paz', 'La Paz Oeste', 'San Pedro Masahuat'),
    ('La Paz', 'La Paz Oeste', 'Tapalhuaca'),
    ('La Paz', 'La Paz Oeste', 'San Francisco Chinameca'),
    -- La Paz Centro (12)
    ('La Paz', 'La Paz Centro', 'El Rosario'),
    ('La Paz', 'La Paz Centro', 'Jerusalén'),
    ('La Paz', 'La Paz Centro', 'Mercedes La Ceiba'),
    ('La Paz', 'La Paz Centro', 'Paraíso de Osorio'),
    ('La Paz', 'La Paz Centro', 'San Antonio Masahuat'),
    ('La Paz', 'La Paz Centro', 'San Emigdio'),
    ('La Paz', 'La Paz Centro', 'San Juan Tepezontes'),
    ('La Paz', 'La Paz Centro', 'San Luis La Herradura'),
    ('La Paz', 'La Paz Centro', 'San Miguel Tepezontes'),
    ('La Paz', 'La Paz Centro', 'San Pedro Nonualco'),
    ('La Paz', 'La Paz Centro', 'Santa María Ostuma'),
    ('La Paz', 'La Paz Centro', 'Santiago Nonualco'),
    -- La Paz Este (3)
    ('La Paz', 'La Paz Este', 'San Juan Nonualco'),
    ('La Paz', 'La Paz Este', 'San Rafael Obrajuelo'),
    ('La Paz', 'La Paz Este', 'Zacatecoluca'),
    -- La Unión Norte (10)
    ('La Unión', 'La Unión Norte', 'Anamorós'),
    ('La Unión', 'La Unión Norte', 'Bolívar'),
    ('La Unión', 'La Unión Norte', 'Concepción de Oriente'),
    ('La Unión', 'La Unión Norte', 'El Sauce'),
    ('La Unión', 'La Unión Norte', 'Lislique'),
    ('La Unión', 'La Unión Norte', 'Nueva Esparta'),
    ('La Unión', 'La Unión Norte', 'Pasaquina'),
    ('La Unión', 'La Unión Norte', 'Polorós'),
    ('La Unión', 'La Unión Norte', 'San José La Fuente'),
    ('La Unión', 'La Unión Norte', 'Santa Rosa de Lima'),
    -- La Unión Sur (8)
    ('La Unión', 'La Unión Sur', 'Conchagua'),
    ('La Unión', 'La Unión Sur', 'El Carmen'),
    ('La Unión', 'La Unión Sur', 'Intipucá'),
    ('La Unión', 'La Unión Sur', 'La Unión'),
    ('La Unión', 'La Unión Sur', 'Meanguera del Golfo'),
    ('La Unión', 'La Unión Sur', 'San Alejo'),
    ('La Unión', 'La Unión Sur', 'Yayantique'),
    ('La Unión', 'La Unión Sur', 'Yucuaiquín'),
    -- Usulután Norte (9)
    ('Usulután', 'Usulután Norte', 'Santiago de María'),
    ('Usulután', 'Usulután Norte', 'Alegría'),
    ('Usulután', 'Usulután Norte', 'Berlín'),
    ('Usulután', 'Usulután Norte', 'Mercedes Umaña'),
    ('Usulután', 'Usulután Norte', 'Jucuapa'),
    ('Usulután', 'Usulután Norte', 'El Triunfo'),
    ('Usulután', 'Usulután Norte', 'Estanzuelas'),
    ('Usulután', 'Usulután Norte', 'San Buenaventura'),
    ('Usulután', 'Usulután Norte', 'Nueva Granada'),
    -- Usulután Este (10)
    ('Usulután', 'Usulután Este', 'Usulután'),
    ('Usulután', 'Usulután Este', 'Jucuarán'),
    ('Usulután', 'Usulután Este', 'San Dionisio'),
    ('Usulután', 'Usulután Este', 'Concepción Batres'),
    ('Usulután', 'Usulután Este', 'Santa María'),
    ('Usulután', 'Usulután Este', 'Ozatlán'),
    ('Usulután', 'Usulután Este', 'Tecapán'),
    ('Usulután', 'Usulután Este', 'Santa Elena'),
    ('Usulután', 'Usulután Este', 'California'),
    ('Usulután', 'Usulután Este', 'Ereguayquín'),
    -- Usulután Oeste (4)
    ('Usulután', 'Usulután Oeste', 'Jiquilisco'),
    ('Usulután', 'Usulután Oeste', 'Puerto El Triunfo'),
    ('Usulután', 'Usulután Oeste', 'San Agustín'),
    ('Usulután', 'Usulután Oeste', 'San Francisco Javier'),
    -- Sonsonate Norte (4)
    ('Sonsonate', 'Sonsonate Norte', 'Juayúa'),
    ('Sonsonate', 'Sonsonate Norte', 'Nahuizalco'),
    ('Sonsonate', 'Sonsonate Norte', 'Salcoatitán'),
    ('Sonsonate', 'Sonsonate Norte', 'Santa Catarina Masahuat'),
    -- Sonsonate Centro (5)
    ('Sonsonate', 'Sonsonate Centro', 'Sonsonate'),
    ('Sonsonate', 'Sonsonate Centro', 'Sonzacate'),
    ('Sonsonate', 'Sonsonate Centro', 'Nahulingo'),
    ('Sonsonate', 'Sonsonate Centro', 'San Antonio del Monte'),
    ('Sonsonate', 'Sonsonate Centro', 'Santo Domingo de Guzmán'),
    -- Sonsonate Este (6)
    ('Sonsonate', 'Sonsonate Este', 'Izalco'),
    ('Sonsonate', 'Sonsonate Este', 'Armenia'),
    ('Sonsonate', 'Sonsonate Este', 'Caluco'),
    ('Sonsonate', 'Sonsonate Este', 'San Julián'),
    ('Sonsonate', 'Sonsonate Este', 'Cuisnahuat'),
    ('Sonsonate', 'Sonsonate Este', 'Santa Isabel Ishuatán'),
    -- Sonsonate Oeste (1)
    ('Sonsonate', 'Sonsonate Oeste', 'Acajutla'),
    -- Santa Ana Norte (4)
    ('Santa Ana', 'Santa Ana Norte', 'Masahuat'),
    ('Santa Ana', 'Santa Ana Norte', 'Metapán'),
    ('Santa Ana', 'Santa Ana Norte', 'Santa Rosa Guachipilín'),
    ('Santa Ana', 'Santa Ana Norte', 'Texistepeque'),
    -- Santa Ana Centro (1)
    ('Santa Ana', 'Santa Ana Centro', 'Santa Ana'),
    -- Santa Ana Este (2)
    ('Santa Ana', 'Santa Ana Este', 'Coatepeque'),
    ('Santa Ana', 'Santa Ana Este', 'El Congo'),
    -- Santa Ana Oeste (6)
    ('Santa Ana', 'Santa Ana Oeste', 'Candelaria de la Frontera'),
    ('Santa Ana', 'Santa Ana Oeste', 'Chalchuapa'),
    ('Santa Ana', 'Santa Ana Oeste', 'El Porvenir'),
    ('Santa Ana', 'Santa Ana Oeste', 'San Antonio Pajonal'),
    ('Santa Ana', 'Santa Ana Oeste', 'San Sebastián Salitrillo'),
    ('Santa Ana', 'Santa Ana Oeste', 'Santiago de la Frontera'),
    -- San Vicente Norte (7)
    ('San Vicente', 'San Vicente Norte', 'Apastepeque'),
    ('San Vicente', 'San Vicente Norte', 'Santa Clara'),
    ('San Vicente', 'San Vicente Norte', 'San Ildefonso'),
    ('San Vicente', 'San Vicente Norte', 'San Esteban Catarina'),
    ('San Vicente', 'San Vicente Norte', 'San Sebastián'),
    ('San Vicente', 'San Vicente Norte', 'San Lorenzo'),
    ('San Vicente', 'San Vicente Norte', 'Santo Domingo'),
    -- San Vicente Sur (6)
    ('San Vicente', 'San Vicente Sur', 'San Vicente'),
    ('San Vicente', 'San Vicente Sur', 'Guadalupe'),
    ('San Vicente', 'San Vicente Sur', 'Verapaz'),
    ('San Vicente', 'San Vicente Sur', 'Tepetitán'),
    ('San Vicente', 'San Vicente Sur', 'Tecoluca'),
    ('San Vicente', 'San Vicente Sur', 'San Cayetano Istepeque'),
    -- San Miguel Norte (8)
    ('San Miguel', 'San Miguel Norte', 'Ciudad Barrios'),
    ('San Miguel', 'San Miguel Norte', 'Sesori'),
    ('San Miguel', 'San Miguel Norte', 'Nuevo Edén de San Juan'),
    ('San Miguel', 'San Miguel Norte', 'San Gerardo'),
    ('San Miguel', 'San Miguel Norte', 'San Luis de la Reina'),
    ('San Miguel', 'San Miguel Norte', 'Carolina'),
    ('San Miguel', 'San Miguel Norte', 'San Antonio del Mosco'),
    ('San Miguel', 'San Miguel Norte', 'Chapeltique'),
    -- San Miguel Centro (6)
    ('San Miguel', 'San Miguel Centro', 'San Miguel'),
    ('San Miguel', 'San Miguel Centro', 'Comacarán'),
    ('San Miguel', 'San Miguel Centro', 'Uluazapa'),
    ('San Miguel', 'San Miguel Centro', 'Moncagua'),
    ('San Miguel', 'San Miguel Centro', 'Quelepa'),
    ('San Miguel', 'San Miguel Centro', 'Chirilagua'),
    -- San Miguel Oeste (6)
    ('San Miguel', 'San Miguel Oeste', 'Chinameca'),
    ('San Miguel', 'San Miguel Oeste', 'Nueva Guadalupe'),
    ('San Miguel', 'San Miguel Oeste', 'Lolotique'),
    ('San Miguel', 'San Miguel Oeste', 'San Jorge'),
    ('San Miguel', 'San Miguel Oeste', 'San Rafael Oriente'),
    ('San Miguel', 'San Miguel Oeste', 'El Tránsito'),
    -- Morazán Norte (11)
    ('Morazán', 'Morazán Norte', 'Arambala'),
    ('Morazán', 'Morazán Norte', 'Cacaopera'),
    ('Morazán', 'Morazán Norte', 'Corinto'),
    ('Morazán', 'Morazán Norte', 'El Rosario'),
    ('Morazán', 'Morazán Norte', 'Joateca'),
    ('Morazán', 'Morazán Norte', 'Jocoaitique'),
    ('Morazán', 'Morazán Norte', 'Meanguera'),
    ('Morazán', 'Morazán Norte', 'Perquín'),
    ('Morazán', 'Morazán Norte', 'San Fernando'),
    ('Morazán', 'Morazán Norte', 'San Isidro'),
    ('Morazán', 'Morazán Norte', 'Torola'),
    -- Morazán Sur (15)
    ('Morazán', 'Morazán Sur', 'Chilanga'),
    ('Morazán', 'Morazán Sur', 'Delicias de Concepción'),
    ('Morazán', 'Morazán Sur', 'El Divisadero'),
    ('Morazán', 'Morazán Sur', 'Gualococti'),
    ('Morazán', 'Morazán Sur', 'Guatajiagua'),
    ('Morazán', 'Morazán Sur', 'Jocoro'),
    ('Morazán', 'Morazán Sur', 'Lolotiquillo'),
    ('Morazán', 'Morazán Sur', 'Osicala'),
    ('Morazán', 'Morazán Sur', 'San Carlos'),
    ('Morazán', 'Morazán Sur', 'San Francisco Gotera'),
    ('Morazán', 'Morazán Sur', 'San Simón'),
    ('Morazán', 'Morazán Sur', 'Sensembra'),
    ('Morazán', 'Morazán Sur', 'Sociedad'),
    ('Morazán', 'Morazán Sur', 'Yamabal'),
    ('Morazán', 'Morazán Sur', 'Yoloaiquín');

INSERT INTO distrito (id_municipio, nombre)
SELECT m.id_municipio, t.distrito
FROM tmp_distrito_v8 t
         JOIN departamento d ON d.nombre = t.departamento
         JOIN municipio m ON m.id_departamento = d.id_departamento
    AND m.nombre = t.municipio
    ON CONFLICT (id_municipio, nombre) DO NOTHING;

DO $$
DECLARE
esperados integer;
    faltantes integer;
BEGIN
SELECT count(*) INTO esperados FROM tmp_distrito_v8;

IF esperados <> 262 THEN
        RAISE EXCEPTION 'Se esperaban 262 distritos en la migracion y hay %', esperados;
END IF;

SELECT count(*) INTO faltantes
FROM tmp_distrito_v8 t
WHERE NOT EXISTS (
    SELECT 1
    FROM distrito x
             JOIN municipio m ON m.id_municipio = x.id_municipio
             JOIN departamento d ON d.id_departamento = m.id_departamento
    WHERE d.nombre = t.departamento
      AND m.nombre = t.municipio
      AND x.nombre = t.distrito
);

IF faltantes <> 0 THEN
        RAISE EXCEPTION 'Faltan % de los 262 distritos: revisar nombres de departamento y municipio', faltantes;
END IF;
END $$;

DROP TABLE tmp_distrito_v8;