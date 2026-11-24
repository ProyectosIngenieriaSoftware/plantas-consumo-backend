package sv.edu.ues.fmp.flora.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean activo;

    /**
     * Si es un rol de sistema, de SOLO LECTURA.
     *
     * <h2>Para que se expone</h2>
     * Para que el frontend pueda marcar y bloquear la fila protegida en la
     * pantalla de roles: en un rol de sistema el boton de renombrar y el de
     * eliminar no tienen por que existir --el backend los rechaza con 409 (R7b)
     * incluso para el superadministrador-- y los de descripcion y permisos solo
     * sirven si quien mira es el superadministrador (R7a, 403 para el resto).
     * Sin este campo la UI ofrecia las cuatro operaciones a todo el mundo para
     * que el backend contestara.
     *
     * <h2>Por que no se puede escribir</h2>
     * Mismo patron que {@code UsuarioResponse.esSuperadmin}, y por las mismas
     * tres razones: {@code RolRequest} no lleva el campo, {@code RolMapper}
     * no lo escribe ni al crear ni al actualizar --asi que un
     * {@code "esSistema": true} en el cuerpo se ignora en silencio, porque
     * {@code FAIL_ON_UNKNOWN_PROPERTIES} esta desactivado-- y el
     * {@code access = READ_ONLY} deja constancia de la intencion en el tipo
     * por si algun dia esta clase se usara como cuerpo de entrada.
     * <p>
     * La marca se setea por migracion ({@code V20}) y por ninguna ruta HTTP.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean esSistema;
}
