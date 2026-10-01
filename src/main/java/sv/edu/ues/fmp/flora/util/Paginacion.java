package sv.edu.ues.fmp.flora.util;

import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import sv.edu.ues.fmp.flora.dto.response.PaginaResponse;
import sv.edu.ues.fmp.flora.exception.ParametroInvalidoException;

/**
 * Utilidad compartida por los endpoints de listado paginado.
 * <p>
 * {@link #armar} valida y construye el {@link Pageable} a partir de los
 * parametros "pagina"/"tamanio" que llegan del controlador; {@link #aRespuesta}
 * convierte el {@link Page} que devuelve el repositorio en un
 * {@link PaginaResponse}, para que ningun servicio repita esta logica ni
 * filtre el tipo Page de Spring Data hacia el cliente de la API.
 */
public final class Paginacion {

    private static final int TAMANIO_MAXIMO = 100;

    private Paginacion() {
    }

    /**
     * @param campoOrden propiedad de la entidad (no columna de BD) por la que
     *                    se ordena, para que la paginacion sea determinista.
     */
    public static Pageable armar(int pagina, int tamanio, String campoOrden) {
        if (pagina < 0) {
            throw new ParametroInvalidoException("El numero de pagina no puede ser negativo");
        }
        if (tamanio < 1 || tamanio > TAMANIO_MAXIMO) {
            throw new ParametroInvalidoException(
                    "El tamanio de pagina debe ser un valor entre 1 y " + TAMANIO_MAXIMO);
        }
        return PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.ASC, campoOrden));
    }

    public static <E, R> PaginaResponse<R> aRespuesta(Page<E> pagina, Function<E, R> mapeo) {
        return PaginaResponse.<R>builder()
                .contenido(pagina.getContent().stream().map(mapeo).toList())
                .pagina(pagina.getNumber())
                .tamanio(pagina.getSize())
                .totalElementos(pagina.getTotalElements())
                .totalPaginas(pagina.getTotalPages())
                .primera(pagina.isFirst())
                .ultima(pagina.isLast())
                .build();
    }
}
