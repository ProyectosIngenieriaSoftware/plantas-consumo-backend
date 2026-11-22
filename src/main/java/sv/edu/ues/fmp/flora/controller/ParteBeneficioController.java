package sv.edu.ues.fmp.flora.controller;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sv.edu.ues.fmp.flora.dto.request.ParteBeneficioRequest;
import sv.edu.ues.fmp.flora.dto.request.ParteBeneficioUpdateRequest;
import sv.edu.ues.fmp.flora.dto.response.ParteBeneficioResponse;
import sv.edu.ues.fmp.flora.service.ParteBeneficioService;

@RestController
@RequestMapping("/api/parte-beneficios")
@RequiredArgsConstructor
@Tag(name = "Parte beneficio", description = "Beneficios asociados a partes comestibles")
public class ParteBeneficioController {
    private final ParteBeneficioService parteBeneficioService;

    /**
     * Listado administrativo: recorre los beneficios de las partes de todas
     * las especies sin mirar el estado de ninguna ficha, asi que sin permiso
     * mostraria los de los borradores. Igual que {@link #listarActivas()}.
     */
    @Operation(summary = "Listar todos los registros de parte beneficio",
            description = "Requiere el permiso ESPECIE_CONSULTAR: es el listado global, incluye los registros de fichas sin publicar. "
                    + "Devuelve los registros activos e inactivos de parte beneficio.")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @ApiResponse(responseCode = "401", description = "Falta el token")
    @ApiResponse(responseCode = "403", description = "El token no trae ESPECIE_CONSULTAR")
    @PreAuthorize("hasAuthority('ESPECIE_CONSULTAR')")
    @GetMapping
    public ResponseEntity<List<ParteBeneficioResponse>> listarTodas() {
        return ResponseEntity.ok(parteBeneficioService.listarTodas());
    }

    /** El mismo listado global que {@link #listarTodas()}, solo que filtrado por activa=true. */
    @Operation(summary = "Listar los registros activos de parte beneficio",
            description = "Requiere el permiso ESPECIE_CONSULTAR: es el listado global, incluye los registros de fichas sin publicar. "
                    + "Devuelve los registros de parte beneficio con activa=true, sin filtrar el estado de la parte, el beneficio o la fuente.")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @ApiResponse(responseCode = "401", description = "Falta el token")
    @ApiResponse(responseCode = "403", description = "El token no trae ESPECIE_CONSULTAR")
    @PreAuthorize("hasAuthority('ESPECIE_CONSULTAR')")
    @GetMapping("/activas")
    public ResponseEntity<List<ParteBeneficioResponse>> listarActivas() {
        return ResponseEntity.ok(parteBeneficioService.listarActivas());
    }

    /**
     * Los beneficios de una parte comestible. Abierto, como el resto de la
     * consulta del catalogo.
     *
     * <h2>Por que no pide ESPECIE_CONSULTAR</h2>
     * Los dos listados de arriba si lo piden, y con razon: son globales --un
     * {@code findAll()} y un {@code findByActivaTrue()}-- y arman la respuesta
     * sin mirar el estado de ninguna ficha. Pero eso dejaba a la ficha publica
     * sin manera de pedir los beneficios de una parte: el front tenia que
     * adivinar cada {@code idBeneficio} para usar
     * {@link #obtenerPorId(Long, Long)}. Faltaba el listado por padre, el
     * hermano de {@code GET /api/preparaciones-consumo/parte/{id}} y de
     * {@code GET /api/epocas-cosecha?idEspecieParte={id}}.
     * <p>
     * Esta ruta si recibe el id de la parte, asi que
     * {@code ParteBeneficioServiceImpl.listarPorParte} puede aplicar
     * {@code VisibilidadDeEspecie} como sus hermanos: los beneficios de una
     * ficha PUBLICADA se ven sin token, y los de una BORRADOR o EN_REVISION
     * responden 404 a quien no traiga {@code ESPECIE_CONSULTAR}. La decision
     * vive en la logica y no en la matriz por ese 404: un 403 sobre el id de
     * una parte confirmaria que esta ocupado por una ficha sin publicar.
     * <p>
     * Los dos listados globales no se tocaron: siguen exigiendo el permiso.
     */
    @Operation(summary = "Listar los beneficios de una parte comestible",
            description = "Listado público por parte comestible: el que usa la ficha del catálogo. "
                    + "Devuelve solo los registros con activa=true, el mismo criterio de /activas. "
                    + "Si la parte existe y no tiene beneficios, devuelve una lista vacía. "
                    + "Los beneficios de una ficha sin publicar solo se ven con el permiso ESPECIE_CONSULTAR; "
                    + "sin él responde el mismo 404 que una parte inexistente.")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @ApiResponse(responseCode = "400", description = "Identificador inválido")
    @ApiResponse(responseCode = "404", description = "La parte comestible no existe o no es visible para quien llama")
    @GetMapping("/parte/{idEspecieParte}")
    public ResponseEntity<List<ParteBeneficioResponse>> listarPorParte(@PathVariable Long idEspecieParte) {
        return ResponseEntity.ok(parteBeneficioService.listarPorParte(idEspecieParte));
    }

    @Operation(summary = "Obtener parte beneficio por ID",
            description = "Indica el ID de la parte comestible y el ID del beneficio. Ambos identifican el registro, que puede estar activo o inactivo.")
    @ApiResponse(responseCode = "200", description = "Parte beneficio encontrado")
    @ApiResponse(responseCode = "400", description = "Identificador inválido")
    @ApiResponse(responseCode = "404", description = "El registro de parte beneficio no existe")
    @GetMapping("/{idEspecieParte}/{idBeneficio}")
    public ResponseEntity<ParteBeneficioResponse> obtenerPorId(@PathVariable Long idEspecieParte,
                                                               @PathVariable Long idBeneficio) {
        return ResponseEntity.ok(parteBeneficioService.obtenerPorId(idEspecieParte, idBeneficio));
    }

    @Operation(summary = "Crear parte beneficio",
            description = "La especie, su parte comestible, el beneficio y la fuente deben existir y estar activos. Los IDs del JSON deben ser enteros sin comillas ni decimales. Fuente y observación "
                    + "son obligatorias. El registro se crea activo. No se permite repetir la misma parte y beneficio, aunque el registro anterior esté inactivo.")
    @ApiResponse(responseCode = "201", description = "Parte beneficio creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o campos obligatorios ausentes")
    @ApiResponse(responseCode = "404", description = "No existe un registro relacionado")
    @ApiResponse(responseCode = "409", description = "Combinación duplicada, relación inactiva o conflicto de integridad")
    @PreAuthorize("hasAuthority('PARTE_GESTIONAR')")
    @PostMapping
    public ResponseEntity<ParteBeneficioResponse> crear(@Valid @RequestBody ParteBeneficioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(parteBeneficioService.crear(request));
    }

    @Operation(summary = "Actualizar parte beneficio",
            description = "Permite editar la fuente, la observación y el estado. Fuente y observación son obligatorias. Los dos IDs de la URL no se modifican. "
                    + "Si omites activa o envías null, se conserva el estado actual. Para dejar el registro activo, "
                    + "la especie, su parte comestible, el beneficio y la fuente deben estar activos. idFuente debe ser un entero sin comillas ni decimales. También puedes editar un registro y mantenerlo inactivo.")
    @ApiResponse(responseCode = "200", description = "Parte beneficio actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "No existe el registro de parte beneficio o un registro relacionado")
    @ApiResponse(responseCode = "409", description = "Relación inactiva o conflicto de integridad")
    @PreAuthorize("hasAuthority('PARTE_GESTIONAR')")
    @PutMapping("/{idEspecieParte}/{idBeneficio}")
    public ResponseEntity<ParteBeneficioResponse> actualizar(@PathVariable Long idEspecieParte,
                                                             @PathVariable Long idBeneficio, @Valid @RequestBody ParteBeneficioUpdateRequest request) {
        return ResponseEntity.ok(parteBeneficioService.actualizar(idEspecieParte, idBeneficio, request));
    }

    @Operation(summary = "Desactivar parte beneficio",
            description = "Cambia el estado a activa=false sin eliminar el registro de la base de datos. Puedes repetir la operación aunque ya esté inactivo.")
    @ApiResponse(responseCode = "204", description = "Parte beneficio desactivado")
    @ApiResponse(responseCode = "400", description = "Identificador inválido")
    @ApiResponse(responseCode = "404", description = "El registro de parte beneficio no existe")
    @PreAuthorize("hasAuthority('PARTE_GESTIONAR')")
    @DeleteMapping("/{idEspecieParte}/{idBeneficio}")
    public ResponseEntity<Void> desactivar(@PathVariable Long idEspecieParte, @PathVariable Long idBeneficio) {
        parteBeneficioService.desactivar(idEspecieParte, idBeneficio);
        return ResponseEntity.noContent().build();
    }
}
