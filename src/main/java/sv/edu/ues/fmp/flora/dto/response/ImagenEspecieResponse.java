package sv.edu.ues.fmp.flora.dto.response;

import java.time.LocalDateTime;
import lombok.*;
import sv.edu.ues.fmp.flora.entity.enums.OrigenImagen;
import sv.edu.ues.fmp.flora.entity.enums.TipoImagen;

/**
 * Representación pública de la imagen sin exponer entidades ni relaciones
 * diferidas.
 *
 * <h2>Lo que no sale: ruta_archivo</h2>
 * El nombre del archivo en disco es un detalle del almacenamiento y no le
 * sirve a ningún cliente: una imagen propia se pide por
 * {@code GET /api/imagenes-especie/{id}/archivo}, con el id de la fila, no con
 * el nombre del archivo. Publicarlo solo diría cómo están organizados los
 * archivos del servidor. Lo que sí sale son los metadatos que un cliente usa
 * para decidir si la descarga --tipo y tamaño-- y para mostrar de dónde viene.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImagenEspecieResponse {
    private Long idImagen;
    private Long idEspecie;
    private Long idEspecieParte;
    private TipoImagen tipoImagen;

    /** PROPIA (archivo en el servidor) o URL (enlace externo). */
    private OrigenImagen origen;

    /** Solo en las de origen URL; null en una imagen propia. */
    private String urlImagen;

    /** Los tres siguientes solo en las de origen PROPIA; null en un enlace. */
    private String nombreOriginal;
    private String tipoContenido;
    private Long tamanioBytes;
    private String titulo;
    private String descripcion;
    private String textoAlternativo;
    private String creditos;
    private Boolean esPrincipal;
    private Boolean activa;
    private LocalDateTime fechaRegistro;
}
