package sv.edu.ues.fmp.flora.entity.enums;

/**
 * Estado del flujo editorial de una ficha de especie.
 * Se persiste como texto en la columna {@code especie.estado_publicacion}
 * (varchar(12)), protegida por el CHECK {@code ck_especie_estado}.
 * <p>
 * Transiciones permitidas por la capa de servicio:
 * <ul>
 *   <li>BORRADOR -> EN_REVISION</li>
 *   <li>RECHAZADA -> EN_REVISION</li>
 *   <li>EN_REVISION -> RECHAZADA</li>
 *   <li>EN_REVISION (ya validada) -> PUBLICADA</li>
 * </ul>
 * Validar no cambia el estado: una especie validada sigue EN_REVISION hasta
 * que alguien la publica.
 */
public enum EstadoPublicacion {
    BORRADOR,
    EN_REVISION,
    PUBLICADA,
    RECHAZADA
}
