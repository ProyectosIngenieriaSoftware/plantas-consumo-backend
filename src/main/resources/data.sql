INSERT INTO usuario
    (id_usuario, nombres, apellidos, correo, nombre_usuario, clave, activo,
     fecha_registro, fecha_actualizacion, ultimo_acceso)
VALUES
    (1, 'Andrea', 'Martinez', 'andrea.martinez@ues.edu.sv', 'andrea.martinez', '$2a$10$38vc6FfEBccgHrvdQj0fye62PdIHVV5csbytJY5EGxJDaFX6lvMRy', true,
     '2026-01-15 08:30:00', '2026-01-15 08:30:00', '2026-09-01 14:12:00'),
    (2, 'Carlos', 'Reyes', 'carlos.reyes@ues.edu.sv', 'carlos.reyes', '$2a$10$38vc6FfEBccgHrvdQj0fye62PdIHVV5csbytJY5EGxJDaFX6lvMRy', true,
     '2026-02-03 09:00:00', '2026-02-03 09:00:00', '2026-08-28 10:05:00'),
    (3, 'Beatriz', 'Lopez', 'beatriz.lopez@ues.edu.sv', 'beatriz.lopez', '$2a$10$38vc6FfEBccgHrvdQj0fye62PdIHVV5csbytJY5EGxJDaFX6lvMRy', true,
     '2026-03-10 11:15:00', '2026-03-10 11:15:00', NULL),
    (4, 'Jose', 'Hernandez', 'jose.hernandez@ues.edu.sv', 'jose.hernandez', '$2a$10$38vc6FfEBccgHrvdQj0fye62PdIHVV5csbytJY5EGxJDaFX6lvMRy', false,
     '2026-01-20 07:45:00', '2026-06-05 16:00:00', '2026-05-30 09:20:00'),
    (5, 'Fatima', 'Gonzalez', 'fatima.gonzalez@ues.edu.sv', 'fatima.gonzalez', '$2a$10$38vc6FfEBccgHrvdQj0fye62PdIHVV5csbytJY5EGxJDaFX6lvMRy', true,
     '2026-04-18 13:30:00', '2026-04-18 13:30:00', NULL);

SELECT setval(pg_get_serial_sequence('usuario', 'id_usuario'), (SELECT MAX(id_usuario) FROM usuario));
