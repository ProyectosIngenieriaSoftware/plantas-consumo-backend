package sv.edu.ues.fmp.flora.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.dto.request.RestablecerClaveRequest;
import sv.edu.ues.fmp.flora.dto.request.SolicitudRecuperacionClaveRequest;
import sv.edu.ues.fmp.flora.service.RecuperacionClaveService;

@Tag(name = "Recuperación de clave", description = "Flujo de \"olvidé mi contraseña\": "
        + "solicitud del enlace por correo y restablecimiento con el token recibido")
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class RecuperacionClaveController {

    private final RecuperacionClaveService recuperacionClaveService;

    @Operation(summary = "Solicitar recuperación de clave",
            description = "Si el correo pertenece a una cuenta activa, envía un enlace de un "
                    + "solo uso válido por tiempo limitado. Responde siempre igual, exista o "
                    + "no el correo, para no revelar qué cuentas están registradas.")
    @ApiResponse(responseCode = "202", description = "Solicitud aceptada")
    @ApiResponse(responseCode = "400", description = "El cuerpo de la petición no es válido")
    @PostMapping("/recuperar-clave")
    public ResponseEntity<Void> solicitarRecuperacion(
            @Valid @RequestBody SolicitudRecuperacionClaveRequest request) {

        recuperacionClaveService.solicitarRecuperacion(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Operation(summary = "Restablecer la clave con un token de recuperación",
            description = "El token viaja en el enlace enviado por correo. Es de un solo uso "
                    + "y expira a los pocos minutos de haberse generado.")
    @ApiResponse(responseCode = "204", description = "Clave restablecida")
    @ApiResponse(responseCode = "400", description = "El token no es válido, ya se usó, ya "
            + "expiró, o el cuerpo de la petición no es válido")
    @PostMapping("/restablecer-clave")
    public ResponseEntity<Void> restablecerClave(@Valid @RequestBody RestablecerClaveRequest request) {
        recuperacionClaveService.restablecerClave(request);
        return ResponseEntity.noContent().build();
    }
}
