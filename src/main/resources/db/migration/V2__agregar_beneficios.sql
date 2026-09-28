-- V2__agregar_beneficios.sql
-- Agrega el registro de beneficios por parte comestible.
-- Aprobado por Ing. Melba Franco (correo del 11/09/2026, opción b).
-- Responde a los puntos 2 y 3 de las consultas al Ing. Dagoberto:
-- los beneficios se registran por parte comestible y son distintos de los nutrientes.
-- Sigue el mismo patrón que nutriente / aporte_nutricional.
-- tipo_beneficio permite clasificar el beneficio; los valores admitidos
-- corresponden al alcance vigente y pueden ampliarse vía ALTER si el
-- alcance cambia en versiones posteriores.

CREATE TABLE beneficio
(
    id_beneficio   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre         varchar(150)         NOT NULL,
    tipo_beneficio varchar(20)          NOT NULL,
    descripcion    text,
    activo         boolean DEFAULT true NOT NULL,
    CONSTRAINT ck_beneficio_tipo CHECK (tipo_beneficio IN
                                        ('NUTRICIONAL', 'FUNCIONAL', 'OTRO'))
);

CREATE UNIQUE INDEX uk_beneficio_nombre_lower
    ON beneficio (lower(nombre));

CREATE TABLE parte_beneficio
(
    id_especie_parte bigint               NOT NULL,
    id_beneficio     bigint               NOT NULL,
    id_fuente        bigint,
    observacion      text,
    activa           boolean DEFAULT true NOT NULL,
    CONSTRAINT parte_beneficio_pkey PRIMARY KEY (id_especie_parte, id_beneficio),
    CONSTRAINT fk_parte_beneficio_parte FOREIGN KEY (id_especie_parte)
        REFERENCES especie_parte_comestible (id_especie_parte),
    CONSTRAINT fk_parte_beneficio_beneficio FOREIGN KEY (id_beneficio)
        REFERENCES beneficio (id_beneficio),
    CONSTRAINT fk_parte_beneficio_fuente FOREIGN KEY (id_fuente)
        REFERENCES fuente (id_fuente)
);

CREATE INDEX idx_parte_beneficio_parte ON parte_beneficio (id_especie_parte);