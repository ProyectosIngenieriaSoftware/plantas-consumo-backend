package sv.edu.ues.fmp.flora.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import sv.edu.ues.fmp.flora.dto.request.ImagenEspecieArchivoRequest;
import sv.edu.ues.fmp.flora.dto.request.ImagenEspecieRequest;
import sv.edu.ues.fmp.flora.dto.response.ImagenEspecieResponse;

/** Operaciones de imágenes de especie con baja lógica y una sola principal activa. */
public interface ImagenEspecieService {
    /** Lista todas las imágenes o únicamente las activas de la especie. */
    List<ImagenEspecieResponse> listarPorEspecie(Long idEspecie, boolean soloActivas);
    /** Obtiene una imagen, aunque esté desactivada. */
    ImagenEspecieResponse obtenerPorId(Long id);
    /** Registra una imagen por enlace externo bajo una especie activa. */
    ImagenEspecieResponse crear(Long idEspecie, ImagenEspecieRequest request);

    /**
     * Sube el archivo de una imagen propia y la registra bajo una especie
     * activa.
     * <p>
     * Valida el tipo real del archivo --por sus primeros bytes, no por lo que
     * declare el cliente-- y el tamaño antes de escribir nada, de modo que un
     * archivo rechazado no deja fila ni archivo. El nombre con el que se guarda
     * lo genera el sistema.
     */
    ImagenEspecieResponse subirArchivo(Long idEspecie, ImagenEspecieArchivoRequest request,
            MultipartFile archivo);

    /**
     * El binario de una imagen propia, si quien llama puede ver la especie a la
     * que pertenece.
     * <p>
     * Aplica la misma visibilidad que el resto de las consultas de imagen y
     * responde el mismo "no encontrado" cuando no: una imagen de un borrador no
     * se distingue de una que no existe. Una imagen de origen URL no tiene
     * archivo que servir.
     */
    ArchivoDeImagen leerArchivo(Long id);
    /** Reemplaza los datos editables de la imagen. */
    ImagenEspecieResponse actualizar(Long id, ImagenEspecieRequest request);
    /** Da de baja sin eliminar físicamente el registro. */
    void desactivar(Long id);
    /** Reactiva sin recuperar el puesto principal; la promoción debe pedirse explícitamente. */
    ImagenEspecieResponse activar(Long id);
    /** Promueve una imagen activa de forma idempotente. */
    ImagenEspecieResponse marcarComoPrincipal(Long id);
}
