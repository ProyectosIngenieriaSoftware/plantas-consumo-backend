package sv.edu.ues.fmp.flora.service;

import java.util.List;
import sv.edu.ues.fmp.flora.dto.request.AporteNutricionalRequest;
import sv.edu.ues.fmp.flora.dto.response.AporteNutricionalResponse;

public interface AporteNutricionalService {
    List<AporteNutricionalResponse> listarPorEspecieParte(Long idEspecieParte);
    List<AporteNutricionalResponse> listarPorNutriente(Long idNutriente); // NUEVO
    AporteNutricionalResponse obtenerPorId(Long id);
    AporteNutricionalResponse crear(AporteNutricionalRequest request);
    AporteNutricionalResponse actualizar(Long id, AporteNutricionalRequest request);
    void eliminar(Long id);
}