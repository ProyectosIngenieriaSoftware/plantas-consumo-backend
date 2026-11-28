package sv.edu.ues.fmp.flora.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.dto.request.ImagenEspecieRequest;
import sv.edu.ues.fmp.flora.dto.response.ImagenEspecieResponse;
import sv.edu.ues.fmp.flora.service.ArchivoDeImagen;
import sv.edu.ues.fmp.flora.service.ImagenEspecieService;

/** Expone consulta, edición, baja lógica, activación y promoción de una imagen existente. */
@Tag(name = "Imágenes de especie", description = "Imágenes por URL y selección de principal")
@RestController
@RequestMapping("/api/imagenes-especie")
@RequiredArgsConstructor
public class ImagenEspecieController {
    private final ImagenEspecieService imagenEspecieService;

    /** Consulta una imagen por su identificador. */
    @Operation(summary = "Obtener una imagen")
    @ApiResponse(responseCode = "200", description = "Imagen encontrada")
    @ApiResponse(responseCode = "400", description = "Identificador inválido")
    @ApiResponse(responseCode = "404", description = "La imagen no existe")
    @GetMapping("/{id}")
    public ResponseEntity<ImagenEspecieResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(imagenEspecieService.obtenerPorId(id));
    }

    /**
     * Sirve el binario de una imagen propia.
     *
     * <h2>Por que lo sirve el controlador y no un recurso estatico</h2>
     * Mapear la carpeta como recurso estatico la pondria entera detras de una
     * URL predecible, servida por el manejador de recursos de Spring: sin
     * pasar por {@code VisibilidadDeEspecie}, de modo que las imagenes de las
     * fichas en BORRADOR y EN_REVISION quedarian publicas para quien supiera o
     * adivinara un nombre. Exactamente el agujero que se cerro con los
     * sub-recursos de especie. Sirviendolo aqui, el archivo hereda la misma
     * regla que la ficha.
     *
     * <h2>La visibilidad y el 404</h2>
     * La decide el servicio: ficha PUBLICADA y activa, la ve cualquiera sin
     * token --esta ruta es GET y entra en los permitidos de
     * {@code SecurityConfig}--; cualquier otro estado necesita
     * {@code ESPECIE_CONSULTAR}, y si no se cumple sale <strong>404</strong>,
     * con el mismo texto que un id de imagen inventado. Nunca 403: el 403
     * confirmaria que ese id esta ocupado y que la ficha existe pero no esta
     * publicada, que es el dato que no se quiere filtrar.
     *
     * <h2>El Content-Type</h2>
     * El del formato que se detecto al subir leyendo los bytes del archivo, no
     * el que declaro el cliente entonces. {@code Content-Disposition: inline}
     * con el nombre original para que el navegador la muestre en vez de
     * descargarla, y para que al guardarla tenga un nombre reconocible en vez
     * del UUID.
     *
     * <h2>Las imagenes de origen URL</h2>
     * No tienen archivo aqui: su binario lo sirve otro servidor y la direccion
     * ya viene en el Response de la imagen. Piden 409, no 404, porque la imagen
     * existe y se puede ver; lo que no aplica es la operacion.
     */
    @Operation(summary = "Servir el archivo de una imagen propia",
            description = "Devuelve el binario con el tipo detectado al subirlo. Aplica la "
                    + "misma visibilidad que la ficha: las imágenes de una especie publicada "
                    + "se sirven sin token, y las de una ficha sin publicar responden 404 "
                    + "--igual que un id inexistente-- salvo con ESPECIE_CONSULTAR. Las "
                    + "imágenes registradas por URL no tienen archivo en el servidor.")
    @ApiResponse(responseCode = "200", description = "Contenido de la imagen")
    @ApiResponse(responseCode = "400", description = "Identificador inválido")
    @ApiResponse(responseCode = "404",
            description = "La imagen no existe, no es visible, o su archivo no está disponible")
    @ApiResponse(responseCode = "409", description = "La imagen es un enlace externo, no un archivo propio")
    @GetMapping("/{id}/archivo")
    public ResponseEntity<byte[]> servirArchivo(@PathVariable Long id) {
        ArchivoDeImagen archivo = imagenEspecieService.leerArchivo(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion(archivo.nombreOriginal()))
                .body(archivo.contenido());
    }

    /**
     * {@code Content-Disposition} con el nombre original si lo hay.
     * <p>
     * El nombre se cita entre comillas y se le quitan las comillas y los saltos
     * de linea que pudiera traer: va en una cabecera HTTP, y un valor con un
     * {@code \r\n} dentro permitiria inyectar otra cabecera. El nombre lo
     * escogio quien subio el archivo, asi que no es un valor de confianza
     * aunque este guardado en la base.
     */
    private static String disposicion(String nombreOriginal) {
        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            return "inline";
        }

        String seguro = nombreOriginal.replaceAll("[\\r\\n\"\\\\]", "_");
        return "inline; filename=\"" + seguro + "\"";
    }

    /** Reemplaza los datos editables sin cambiar la especie ni el estado. */
    @Operation(summary = "Actualizar una imagen",
            description = "Reemplaza los campos editables. Los opcionales omitidos quedan en null y "
                    + "esPrincipal omitido o nulo equivale a false. La especie no cambia; la parte "
                    + "comestible opcional debe pertenecer a esa misma especie.")
    @ApiResponse(responseCode = "200", description = "Imagen actualizada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o URL incorrecta")
    @ApiResponse(responseCode = "404", description = "La imagen o la parte comestible no existe")
    @ApiResponse(responseCode = "409", description = "Estado inválido, parte ajena o conflicto de integridad")
    @PreAuthorize("hasAuthority('MULTIMEDIA_GESTIONAR')")
    @PutMapping("/{id}")
    public ResponseEntity<ImagenEspecieResponse> actualizar(@PathVariable Long id,
            @Valid @RequestBody ImagenEspecieRequest request) {
        return ResponseEntity.ok(imagenEspecieService.actualizar(id, request));
    }

    /** Permite repetir la baja; si era principal pierde dicha condición. */
    @Operation(summary = "Desactivar una imagen",
            description = "Baja lógica idempotente; no elimina la fila. Si era principal, pierde dicha condición.")
    @ApiResponse(responseCode = "204", description = "Imagen desactivada")
    @ApiResponse(responseCode = "400", description = "Identificador inválido")
    @ApiResponse(responseCode = "404", description = "La imagen no existe")
    @PreAuthorize("hasAuthority('MULTIMEDIA_GESTIONAR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        imagenEspecieService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    /** Reactiva la imagen como no-principal. */
    @Operation(summary = "Activar una imagen",
            description = "Idempotente. La imagen se reactiva como no-principal.")
    @ApiResponse(responseCode = "200", description = "Imagen activa")
    @ApiResponse(responseCode = "400", description = "Identificador inválido")
    @ApiResponse(responseCode = "404", description = "La imagen o la parte comestible no existe")
    @ApiResponse(responseCode = "409", description = "Especie o parte desactivada, parte ajena o conflicto de integridad")
    @PreAuthorize("hasAuthority('MULTIMEDIA_GESTIONAR')")
    @PatchMapping("/{id}/activar")
    public ResponseEntity<ImagenEspecieResponse> activar(@PathVariable Long id) {
        return ResponseEntity.ok(imagenEspecieService.activar(id));
    }

    /** Promueve una imagen activa; repetir la operación conserva la principal. */
    @Operation(summary = "Marcar una imagen como principal",
            description = "Idempotente. La imagen debe estar activa y desplaza a la principal anterior.")
    @ApiResponse(responseCode = "200", description = "Imagen principal")
    @ApiResponse(responseCode = "400", description = "Identificador inválido")
    @ApiResponse(responseCode = "404", description = "La imagen no existe")
    @ApiResponse(responseCode = "409", description = "Imagen o especie desactivada, o conflicto de integridad")
    @PreAuthorize("hasAuthority('MULTIMEDIA_GESTIONAR')")
    @PatchMapping("/{id}/marcar-principal")
    public ResponseEntity<ImagenEspecieResponse> marcarComoPrincipal(@PathVariable Long id) {
        return ResponseEntity.ok(imagenEspecieService.marcarComoPrincipal(id));
    }
}
