-- V6__token_recuperacion_clave.sql
-- Tokens de un solo uso para el flujo de "olvide mi contrasenia".
-- Se guarda el hash SHA-256 del token, no el valor que recibe el usuario por
-- correo, igual que clave_hash en usuario: quien solo tiene acceso a la base
-- de datos no puede reconstruir un enlace de recuperacion valido.

CREATE TABLE token_recuperacion_clave
(
    id_token         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario       bigint                  NOT NULL,
    token_hash       varchar(64)             NOT NULL,
    fecha_expiracion timestamp               NOT NULL,
    usado            boolean DEFAULT false   NOT NULL,
    fecha_creacion   timestamp DEFAULT now() NOT NULL,
    CONSTRAINT uk_token_recuperacion_hash UNIQUE (token_hash),
    CONSTRAINT fk_token_recuperacion_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
);

CREATE INDEX idx_token_recuperacion_usuario ON token_recuperacion_clave (id_usuario);
