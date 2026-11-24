package sv.edu.ues.fmp.flora.service;

import sv.edu.ues.fmp.flora.dto.request.RestablecerClaveRequest;
import sv.edu.ues.fmp.flora.dto.request.SolicitudRecuperacionClaveRequest;

public interface RecuperacionClaveService {

    void solicitarRecuperacion(SolicitudRecuperacionClaveRequest request);

    void restablecerClave(RestablecerClaveRequest request);
}
