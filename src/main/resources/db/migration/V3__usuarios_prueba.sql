-- V3__usuarios_prueba.sql
-- Usuarios de prueba para desarrollo. NO son datos reales del sistema.
-- Todos comparten la misma contraseña hasheada.

INSERT INTO usuario (nombres, apellidos, correo, nombre_usuario, clave_hash, activo)
VALUES
    ('Admin', 'Prueba', 'admin@prueba.local', 'admin',
     '$2a$10$38vc6FfEBccgHrvdQj0fye62PdIHVV5csbytJY5EGxJDaFX6lvMRy', true),
    ('Investigador', 'Prueba', 'investigador@prueba.local', 'investigador',
     '$2a$10$38vc6FfEBccgHrvdQj0fye62PdIHVV5csbytJY5EGxJDaFX6lvMRy', true);

SELECT setval(pg_get_serial_sequence('usuario', 'id_usuario'),
              (SELECT MAX(id_usuario) FROM usuario));