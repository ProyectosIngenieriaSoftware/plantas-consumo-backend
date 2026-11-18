package sv.edu.ues.fmp.flora.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class UsuarioResponse {
    private Long id;
    private String nombres;
    private String apellidos;
    private String correo;
    private String nombreUsuario;
    private Boolean activo;

    /**
     * La marca del superadministrador, de SOLO LECTURA.
     *
     * <h2>Para que se expone</h2>
     * Para que el frontend pueda marcar y bloquear la fila protegida en la UI.
     * Hasta ahora no salia en ninguna respuesta, asi que la pantalla de
     * usuarios no tenia forma de saber a quien no se le puede tocar nada y
     * ofrecia los botones de editar y dar de baja a todo el mundo, para que el
     * backend contestara 403 (R4 y R6 de
     * {@code ProteccionDeAdministracion}).
     *
     * <h2>Por que no se puede escribir</h2>
     * Por dos razones independientes, y las dos tienen que seguir siendo
     * verdad: ningun DTO de entrada la lleva --ni
     * {@code UsuarioCreationRequest} ni {@code UsuarioUpdateRequest} ni
     * {@code UsuarioCambioCredencialesRequest}-- y
     * {@code UsuarioMapper.toEntity} no la toca, asi que un
     * {@code "esSuperadmin": true} en el cuerpo se ignora en silencio
     * ({@code FAIL_ON_UNKNOWN_PROPERTIES} esta desactivado). La marca se setea
     * por migracion ({@code V19}) y por ninguna ruta HTTP.
     * <p>
     * El {@code access = READ_ONLY} es la tercera: esta clase no se
     * deserializa hoy, pero si algun dia alguien la reutilizara como cuerpo de
     * entrada, Jackson ignoraria este campo en vez de aceptarlo.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean esSuperadmin;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime fechaRegistro;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime fechaActualizacion;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime ultimoAcceso;
}
