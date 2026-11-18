package sv.edu.ues.fmp.flora.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * El cuerpo de {@code PUT /api/usuarios/{id}}: la edicion por administrador.
 *
 * <h2>Por que solo nombres y apellidos</h2>
 * Este DTO llevaba tambien {@code correo} y {@code nombreUsuario}, y con ellos
 * un administrador reescribia las credenciales de identidad de cualquier otra
 * cuenta. Como {@code POST /api/usuarios/recuperar-clave} es publico y resuelve
 * la cuenta por el correo, apuntar el correo de otro a un buzon propio y pedir
 * la recuperacion entregaba esa cuenta --incluida la del superadministrador, a
 * la que R4 protegia de la baja pero nadie de la edicion--. No era una
 * desactivacion sino una suplantacion, y no pasaba por ninguna de las reglas de
 * {@code ProteccionDeAdministracion}.
 * <p>
 * Los dos campos se fueron a {@link UsuarioCambioCredencialesRequest}, que es
 * autoservicio: solo el dueño, autenticado como el mismo y con su clave actual.
 * Asi la cadena de recuperacion deja de poder repuntarse en cualquier cuenta,
 * no solo en la del superadministrador.
 *
 * <h2>Lo que este DTO nunca llevo</h2>
 * {@code activo}, {@code es_superadmin}, la clave y los roles. Cada uno tiene
 * su via: el DELETE y el PATCH /activar, ninguna (se setea por migracion), el
 * PUT /clave y {@code /api/usuario-roles}. Un cuerpo que los traiga de todas
 * formas no hace nada: {@code FAIL_ON_UNKNOWN_PROPERTIES} esta desactivado y
 * {@code UsuarioMapper.updateEntity} solo escribe los dos campos de aqui.
 *
 * <h2>La creacion no cambio</h2>
 * {@code UsuarioCreationRequest} sigue fijando el correo y el nombre de
 * usuario: dar de alta una cuenta es precisamente decidir cuales son, y ahi no
 * hay dueño previo al que suplantar.
 */
public record UsuarioUpdateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String nombres,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
        String apellidos
) {
    public UsuarioUpdateRequest {
        nombres = nombres == null ? null : nombres.trim();
        apellidos = apellidos == null ? null : apellidos.trim();
    }
}
