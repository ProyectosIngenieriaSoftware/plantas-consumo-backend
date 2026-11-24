package sv.edu.ues.fmp.flora.entity.enums;

/**
 * Tipo de fuente bibliografica o de conocimiento.
 * Se persiste como texto en la columna {@code fuente.tipo_fuente} (varchar(22)),
 * protegida por un CHECK constraint en la base de datos.
 */
public enum TipoFuente {
    LIBRO,
    ARTICULO,
    SITIO_WEB,
    ENTREVISTA,
    TESIS,
    INFORME,
    CONOCIMIENTO_ANCESTRAL,
    OTRO
}
