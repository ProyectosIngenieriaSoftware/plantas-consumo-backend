package sv.edu.ues.fmp.flora.service;

import java.util.List;
import sv.edu.ues.fmp.flora.dto.request.AporteNutricionalRequest;
import sv.edu.ues.fmp.flora.dto.response.AporteNutricionalResponse;


public interface AporteNutricionalService {
    List<AporteNutricionalResponse> listarPorEspecieParte(Long idEspecieParte);
    List<AporteNutricionalResponse> listarPorNutriente(Long idNutriente); // NUEVO

    /** Como {@link #listarPorNutriente(Long)}, pero sin los dados de baja. */
    List<AporteNutricionalResponse> listarActivosPorNutriente(Long idNutriente);

    AporteNutricionalResponse obtenerPorId(Long id);
    AporteNutricionalResponse crear(AporteNutricionalRequest request);
    AporteNutricionalResponse actualizar(Long id, AporteNutricionalRequest request);

    /**
     * Da de baja logica el aporte: {@code activo = false}. Antes se llamaba
     * {@code eliminar} y borraba la fila; se renombro con el cambio de
     * contrato para que el nombre no mintiera, igual que el
     * {@code desactivar} de {@code BeneficioService}.
     */
    void desactivar(Long id);

    /** Devuelve un aporte retirado al catalogo. Idempotente. */
    AporteNutricionalResponse activar(Long id);
}