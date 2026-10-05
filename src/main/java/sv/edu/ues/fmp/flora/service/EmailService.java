package sv.edu.ues.fmp.flora.service;

public interface EmailService {

    void enviarCorreoRecuperacion(String correoDestino, String nombreDestino, String enlace);
}
