package sv.edu.ues.fmp.flora.service;

import java.util.List;

import sv.edu.ues.fmp.flora.dto.request.EspecieRequest;
import sv.edu.ues.fmp.flora.dto.request.TaxonomiaRequest;
import sv.edu.ues.fmp.flora.dto.response.EspecieResponse;
import sv.edu.ues.fmp.flora.entity.enums.EstadoPublicacion;

/**
 * Contrato de negocio de las especies, incluido su flujo editorial.
 * Trabaja solo con DTOs: la entidad no cruza hacia el controlador.
 * <p>
 * El flujo de publicacion es: BORRADOR -> EN_REVISION -> (validar) ->
 * PUBLICADA, con salida a RECHAZADA desde EN_REVISION y vuelta a EN_REVISION
 * desde RECHAZADA.
 */
public interface EspecieService {

    List<EspecieResponse> listarTodas();

    /** Catalogo publico: solo fichas PUBLICADA y activas. */
    List<EspecieResponse> listarPublicadas();

    List<EspecieResponse> listarPorEstado(EstadoPublicacion estado);

    EspecieResponse obtenerPorId(Long id);

    /**
     * Detalle para el catalogo publico: devuelve la ficha solo si esta
     * PUBLICADA y activa.
     * <p>
     * Existe aparte de {@link #obtenerPorId(Long)} porque ese devuelve
     * cualquier estado, borradores incluidos, y es el que lee el personal
     * interno. Este es el que puede quedar abierto al anonimo.
     * <p>
     * Cuando la ficha existe pero no esta publicada lanza la misma
     * {@code RecursoNoEncontradoException} que cuando el id no existe, y con
     * el mismo texto. Es deliberado: si el mensaje distinguiera "no existe" de
     * "existe pero esta en borrador", el endpoint serviria para averiguar que
     * ids hay en preparacion probando numeros.
     *
     * @throws sv.edu.ues.fmp.flora.exception.RecursoNoEncontradoException
     *         si no hay una especie publicada y activa con ese id
     */
    EspecieResponse obtenerPublicadaPorId(Long id);

    /**
     * Crea la especie y su taxonomia en la misma transaccion.
     * <p>
     * El creador ({@code creada_por}) es siempre el usuario autenticado. No se
     * recibe como parametro ni se lee del Request a proposito: ver
     * {@code UsuarioAutenticado}.
     */
    EspecieResponse crear(EspecieRequest request);

    /** Actualiza los campos editables de la ficha y su taxonomia. */
    /**
     * Reescribe el contenido propio de la ficha. La lista de nombres comunes se
     * ignora aunque venga en el request: se gestiona por sus propios endpoints.
     * <p>
     * Editar invalida la revision previa: una ficha PUBLICADA vuelve a
     * EN_REVISION y pierde validacion y publicacion, y una ya validada pierde
     * la validacion. El detalle y el motivo, en {@code invalidarRevisionPrevia}.
     * <p>
     * Si la ficha esta PUBLICADA, {@code confirmar} tiene que venir en
     * {@code true}: sacarla del catalogo publico no debe pasar en silencio. Con
     * {@code false} no se cambia nada y sale un 409 con
     * {@code error: "Confirmación requerida"}. En los demas estados el
     * parametro se ignora. Ver {@code exigirConfirmacionSiEstaPublicada}.
     *
     * @param confirmar que quien llama acepta la consecuencia de editar una
     *                  ficha publicada
     */
    EspecieResponse actualizar(Long id, EspecieRequest request, boolean confirmar);

    /**
     * Rectifica solo la clasificacion taxonomica, sin tocar el resto de la
     * ficha. Evita tener que reenviar la especie completa para corregir un
     * genero o una familia.
     * <p>
     * Es una edicion de contenido, asi que invalida la revision previa y pide
     * confirmacion sobre una ficha publicada, igual que
     * {@link #actualizar(Long, EspecieRequest, boolean)}.
     *
     * @param confirmar que quien llama acepta la consecuencia de editar una
     *                  ficha publicada
     */
    EspecieResponse actualizarTaxonomia(Long idEspecie, TaxonomiaRequest request,
            boolean confirmar);

    EspecieResponse enviarARevision(Long id);

    /**
     * Marca la ficha como revisada sin publicarla todavia: el estado sigue
     * siendo EN_REVISION.
     * <p>
     * Se valida una sola vez. Una ficha que ya tiene validador no se puede
     * revalidar, aunque siga EN_REVISION: esa regla cierra la ventana en la
     * que dos validadores podian sobrescribirse mutuamente y dejar la ficha
     * atribuida a quien llamara de ultimo. Para cambiar de validador hay que
     * rechazar la ficha primero, que es la unica transicion que limpia
     * {@code validadaPor}, y volver a enviarla a revision.
     * <p>
     * El validador es el usuario autenticado, igual que el creador en
     * {@link #crear(EspecieRequest)}: antes llegaba como parametro de query y
     * eso permitia firmar la validacion en nombre de otro.
     * <p>
     * Precondicion de datos: la taxonomia tiene que decir la familia. Es
     * decision del equipo con Agronomia, y por eso no esta en el DTO: en
     * BORRADOR y EN_REVISION la familia sigue siendo opcional, y lo que la
     * exige es esta transicion. Sin familia sale 409 y la ficha no se valida.
     */
    EspecieResponse validar(Long id);

    /**
     * Publica una ficha ya validada. El publicador es el usuario autenticado,
     * por el mismo motivo que el validador en {@link #validar(Long)}.
     * <p>
     * Repite la compuerta de familia de {@link #validar(Long)}, y es la que de
     * verdad garantiza que nada publicado quede sin familia: entre validar y
     * publicar, un {@code ESPECIE_EDITAR} puede vaciarla por PUT o por
     * PATCH /taxonomia, que no tienen guarda de estado. Sin familia sale 409 y
     * la ficha se queda EN_REVISION.
     */
    EspecieResponse publicar(Long id);

    EspecieResponse rechazar(Long id);

    /**
     * Baja logica. Nunca DELETE fisico: varias tablas referencian la especie.
     * <p>
     * Una especie PUBLICADA no se puede dar de baja: lanza
     * {@code EstadoInvalidoException} (409). Para retirarla del catalogo hay
     * que sacarla antes del estado PUBLICADA por la via editorial. BORRADOR,
     * EN_REVISION y RECHAZADA se dan de baja sin restriccion.
     */
    void desactivar(Long id);

    /**
     * Revierte la baja logica. Idempotente. Conserva el estado de publicacion,
     * asi que una especie PUBLICADA vuelve al catalogo publico de inmediato.
     */
    EspecieResponse activar(Long id);
}
