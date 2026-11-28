package sv.edu.ues.fmp.flora.entity.enums;

/**
 * De donde sale el binario de una imagen de especie.
 *
 * <h2>Por que hace falta la columna y no basta mirar si hay url</h2>
 * Se podria deducir del par de columnas --si {@code url_imagen} tiene algo es
 * un enlace, si lo que tiene algo es {@code ruta_archivo} es un archivo-- pero
 * entonces la regla viviria implicita en dos columnas nulables y cualquier fila
 * con las dos llenas, o con las dos vacias, seria un dato sin interpretacion.
 * Con la columna explicita, el CHECK {@code ck_imagen_fuente_unica} puede
 * exigir que el origen y la fuente presente coincidan, y la fila se lee sin
 * tener que inferir nada.
 *
 * <h2>Los dos valores</h2>
 * <ul>
 *   <li>{@link #URL} -- enlace externo, lo que habia hasta ahora. El binario no
 *       es nuestro: lo sirve otro servidor y nosotros guardamos la direccion.
 *       Las 18 filas que existian al hacer el cambio son todas URL, asi las
 *       dejo el backfill de {@code V22}.</li>
 *   <li>{@link #PROPIA} -- el archivo lo subio alguien del equipo y vive en la
 *       carpeta de almacenamiento del servidor. En la base queda la referencia
 *       ({@code ruta_archivo}, el nombre que genera el sistema) y los metadatos
 *       que hacen falta para servirlo y para mostrarlo en una lista.</li>
 * </ul>
 * Los nombres estan en {@code ck_imagen_origen}: anadir un valor aqui sin
 * tocar ese CHECK hace que la fila la rechace la base.
 */
public enum OrigenImagen {
    /** Archivo subido por el equipo, almacenado en el servidor. */
    PROPIA,
    /** Enlace externo; el binario lo sirve otro servidor. */
    URL
}
