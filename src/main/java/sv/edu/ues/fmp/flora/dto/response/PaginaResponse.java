package sv.edu.ues.fmp.flora.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Envoltorio uniforme para cualquier listado paginado de la API.
 * <p>
 * Se arma con {@link sv.edu.ues.fmp.flora.util.Paginacion#aRespuesta} a
 * partir de un {@link org.springframework.data.domain.Page} de Spring Data,
 * para no exponer ese tipo (ni su serializacion por defecto) directamente en
 * los controladores.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginaResponse<T> {
    private List<T> contenido;
    private int pagina;
    private int tamanio;
    private long totalElementos;
    private int totalPaginas;
    private boolean primera;
    private boolean ultima;
}
