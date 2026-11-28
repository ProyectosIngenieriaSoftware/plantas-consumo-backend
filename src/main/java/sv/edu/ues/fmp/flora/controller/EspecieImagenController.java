package sv.edu.ues.fmp.flora.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.dto.request.ImagenEspecieArchivoRequest;
import sv.edu.ues.fmp.flora.dto.request.ImagenEspecieRequest;
import sv.edu.ues.fmp.flora.dto.response.ImagenEspecieResponse;
import sv.edu.ues.fmp.flora.service.ImagenEspecieService;
import sv.edu.ues.fmp.flora.service.VisibilidadDeEspecie;

/** Expone el listado y la creación de imágenes dentro de una especie. */
@Tag(name = "Imágenes de especie", description = "Imágenes por URL y selección de principal")
@RestController
@RequestMapping("/api/especies/{idEspecie}/imagenes")
@RequiredArgsConstructor
public class EspecieImagenController {
    private final ImagenEspecieService imagenEspecieService;

    /**
     * Decide si quien llama puede ver el contenido de esta especie: publicada
     * para cualquiera, sin publicar solo con ESPECIE_CONSULTAR, y 404 en el
     * resto para no delatar los borradores.
     */
    private final VisibilidadDeEspecie visibilidadDeEspecie;

    /** Delega la consulta del historial o de las imágenes activas. */
    @Operation(summary = "Listar imágenes de una especie",
            description = "Con soloActivas=true excluye las imágenes dadas de baja lógica.")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @ApiResponse(responseCode = "400", description = "Identificador o filtro inválido")
    @ApiResponse(responseCode = "404", description = "La especie no existe")
    @GetMapping
    public ResponseEntity<List<ImagenEspecieResponse>> listar(
            @PathVariable Long idEspecie,
            @RequestParam(name = "soloActivas", defaultValue = "false") boolean soloActivas) {
        visibilidadDeEspecie.verificarQuePuedeVer(idEspecie);

        return ResponseEntity.ok(imagenEspecieService.listarPorEspecie(idEspecie, soloActivas));
    }

    /** Registra una URL y devuelve la imagen con la fecha asignada por PostgreSQL. */
    @Operation(summary = "Registrar una imagen de especie",
            description = "Recibe una URL HTTP(S), no archivos. Si es principal desplaza a la anterior.")
    @ApiResponse(responseCode = "201", description = "Imagen registrada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o URL incorrecta")
    @ApiResponse(responseCode = "404", description = "La especie o la parte comestible no existe")
    @ApiResponse(responseCode = "409", description = "Especie o parte desactivada, parte ajena o conflicto de integridad")
    @PreAuthorize("hasAuthority('MULTIMEDIA_GESTIONAR')")
    @PostMapping
    public ResponseEntity<ImagenEspecieResponse> crear(@PathVariable Long idEspecie,
            @Valid @RequestBody ImagenEspecieRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(imagenEspecieService.crear(idEspecie, request));
    }

    /**
     * Sube el archivo de una imagen propia de la especie.
     *
     * <h2>Por que una ruta aparte y no el mismo POST</h2>
     * El POST de arriba consume JSON y exige la URL; este consume
     * {@code multipart/form-data} y exige el archivo. Son dos contratos
     * distintos --cuerpo, validaciones y codigo de error del tipo de
     * contenido-- y un unico endpoint que aceptara los dos tendria que elegir
     * cual valida segun lo que recibio, dejando el 400 de "falta la URL" y el
     * de "falta el archivo" dependiendo del {@code Content-Type}. Separados,
     * cada uno dice exactamente lo que le falta.
     *
     * <h2>Mismo permiso que registrar un enlace</h2>
     * {@code MULTIMEDIA_GESTIONAR}: anadir contenido multimedia a una ficha es
     * la misma operacion del mismo rol, y que el binario venga del disco de
     * quien sube en vez de otro servidor no cambia quien deberia poder hacerlo.
     *
     * <h2>Subir una imagen no manda la ficha a revision</h2>
     * Una imagen es un sub-recurso, igual que hoy: la ficha PUBLICADA sigue
     * PUBLICADA y sigue en el catalogo. Lo unico que cambia en la especie es su
     * {@code fecha_actualizacion}, que la refresca el trigger
     * {@code trg_imagen_especie_toca_especie} (V23) sin tocar el estado.
     *
     * <h2>Los codigos</h2>
     * El 400 cubre dos cosas distintas que conviene no confundir: los
     * metadatos invalidos (falta el tipo de imagen, falta el texto alternativo)
     * y el archivo que no es una imagen admitida --su contenido no lleva la
     * firma de JPG, PNG ni WebP, o el tipo que declaro el cliente no coincide
     * con lo que dicen sus bytes--. El 413 es solo el tamano, y lo pone el
     * parser de multipart antes de llegar aqui.
     */
    @Operation(summary = "Subir una imagen propia de la especie",
            description = "Recibe el archivo en multipart/form-data junto con sus metadatos. "
                    + "Formatos admitidos: JPG, PNG y WebP, hasta 20 MB. El tipo se comprueba "
                    + "leyendo el contenido del archivo, no la extensión ni el tipo declarado. "
                    + "El archivo se guarda en el servidor con un nombre generado por el "
                    + "sistema y se sirve en GET /api/imagenes-especie/{id}/archivo. Subir una "
                    + "imagen no cambia el estado de publicación de la ficha.")
    @ApiResponse(responseCode = "201", description = "Imagen subida y registrada")
    @ApiResponse(responseCode = "400",
            description = "Metadatos inválidos, o el archivo no es una imagen JPG, PNG o WebP")
    @ApiResponse(responseCode = "404", description = "La especie o la parte comestible no existe")
    @ApiResponse(responseCode = "409", description = "Especie o parte desactivada, parte ajena o conflicto de integridad")
    @ApiResponse(responseCode = "413", description = "El archivo excede el tamaño máximo permitido")
    @PreAuthorize("hasAuthority('MULTIMEDIA_GESTIONAR')")
    @PostMapping(path = "/archivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImagenEspecieResponse> subirArchivo(
            @PathVariable Long idEspecie,
            @RequestPart("archivo") MultipartFile archivo,
            @Valid @ModelAttribute ImagenEspecieArchivoRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(imagenEspecieService.subirArchivo(idEspecie, request, archivo));
    }
}
