package sv.edu.ues.fmp.flora.service;

/**
 * El binario de una imagen propia y lo que hace falta para servirlo.
 *
 * <h2>Por que no se devuelve la entidad</h2>
 * El controlador necesita dos cosas --los bytes y el {@code Content-Type}-- y
 * las necesita <em>despues</em> de que la transaccion cerro, porque es ahi
 * donde arma el {@code ResponseEntity}. Devolver la {@code ImagenEspecie}
 * obligaria a leer su {@code tipoContenido} fuera de la transaccion y a que el
 * controlador supiera de entidades; un registro inmutable con lo justo deja la
 * frontera donde esta en el resto del proyecto, que nunca devuelve entidades.
 *
 * @param contenido     los bytes tal como estan en disco
 * @param tipoContenido el tipo detectado al subir, no el declarado entonces por
 *                      el cliente
 * @param nombreOriginal como se llamaba el archivo al subirlo; va en la
 *                      cabecera {@code Content-Disposition} para que al
 *                      descargarlo tenga un nombre reconocible en vez del UUID
 */
public record ArchivoDeImagen(byte[] contenido, String tipoContenido, String nombreOriginal) {
}
