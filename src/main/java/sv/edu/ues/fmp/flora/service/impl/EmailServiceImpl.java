package sv.edu.ues.fmp.flora.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.service.EmailService;

/**
 * Envio de correos transaccionales via {@link JavaMailSender}.
 * <p>
 * JavaMailSender solo habla el protocolo SMTP: el proveedor real (Gmail,
 * Mailtrap, Brevo...) se configura por propiedades ({@code spring.mail.*})
 * en application.properties, nunca en este codigo. Cambiar de proveedor es
 * cambiar esas propiedades, no esta clase.
 */
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.recuperacion-clave.remitente}")
    private String remitente;

    @Value("${app.recuperacion-clave.expiracion-minutos}")
    private int expiracionMinutos;

    @Override
    public void enviarCorreoRecuperacion(String correoDestino, String nombreDestino, String enlace) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(correoDestino);
        mensaje.setSubject("Recuperación de contraseña - Catálogo Botánico");
        mensaje.setText(
                "Hola " + nombreDestino + ",\n\n"
                        + "Recibimos una solicitud para restablecer tu contraseña.\n"
                        + "Si fuiste tú, usa el siguiente enlace (válido por "
                        + expiracionMinutos + " minutos):\n\n"
                        + enlace + "\n\n"
                        + "Si no solicitaste este cambio, puedes ignorar este correo: "
                        + "tu contraseña actual sigue siendo válida.\n\n"
                        + "Catálogo Botánico"
        );
        mailSender.send(mensaje);
    }
}
