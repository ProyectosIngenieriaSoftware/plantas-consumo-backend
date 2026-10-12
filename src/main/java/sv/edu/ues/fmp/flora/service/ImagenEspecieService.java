package sv.edu.ues.fmp.flora.service;

import java.util.List;
import sv.edu.ues.fmp.flora.dto.request.ImagenEspecieRequest;
import sv.edu.ues.fmp.flora.dto.response.ImagenEspecieResponse;

/** Operaciones de imágenes de especie con baja lógica y una sola principal activa. */
public interface ImagenEspecieService {
    /** Lista todas las imágenes o únicamente las activas de la especie. */
    List<ImagenEspecieResponse> listarPorEspecie(Long idEspecie, boolean soloActivas);
    /** Obtiene una imagen, aunque esté desactivada. */
    ImagenEspecieResponse obtenerPorId(Long id);
    /** Registra una imagen bajo una especie activa. */
    ImagenEspecieResponse crear(Long idEspecie, ImagenEspecieRequest request);
    /** Reemplaza los datos editables de la imagen. */
    ImagenEspecieResponse actualizar(Long id, ImagenEspecieRequest request);
    /** Da de baja sin eliminar físicamente el registro. */
    void desactivar(Long id);
    /** Reactiva y recupera el puesto principal cuando corresponda. */
    ImagenEspecieResponse activar(Long id);
    /** Promueve una imagen activa de forma idempotente. */
    ImagenEspecieResponse marcarComoPrincipal(Long id);
}
