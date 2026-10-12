package sv.edu.ues.fmp.flora.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sv.edu.ues.fmp.flora.dto.request.UbicacionEspecieRequestDTO;
import sv.edu.ues.fmp.flora.dto.response.UbicacionEspecieResponseDTO;
import sv.edu.ues.fmp.flora.service.UbicacionEspecieService;

import java.util.List;

@RestController
@RequestMapping("/api/ubicaciones")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Ubicaciones de Especies", description = "Endpoints para gestionar los avistamientos y distribución geográfica (Distritos) de las especies")
public class UbicacionEspecieController {

    private final UbicacionEspecieService ubicacionEspecieService;

    @Operation(summary = "Registrar nueva ubicación", description = "Guarda una nueva ubicación o avistamiento para una especie vinculándola con un distrito")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ubicación registrada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de petición inválidos o incompletos"),
            @ApiResponse(responseCode = "404", description = "La Especie o el Distrito no fueron encontrados en la base de datos")
    })
    @PostMapping
    public ResponseEntity<UbicacionEspecieResponseDTO> registrarUbicacion(@Valid @RequestBody UbicacionEspecieRequestDTO dto) {
        UbicacionEspecieResponseDTO response = ubicacionEspecieService.registrarUbicacion(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Obtener ubicaciones por especie", description = "Devuelve una lista de todas las ubicaciones activas asociadas a una especie específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de ubicaciones recuperada exitosamente")
    })
    @GetMapping("/especie/{idEspecie}")
    public ResponseEntity<List<UbicacionEspecieResponseDTO>> obtenerPorEspecie(@PathVariable Long idEspecie) {
        List<UbicacionEspecieResponseDTO> ubicaciones = ubicacionEspecieService.obtenerUbicacionesPorEspecie(idEspecie);
        return ResponseEntity.ok(ubicaciones);
    }

    @Operation(summary = "Actualizar una ubicación", description = "Actualiza los datos de un avistamiento existente usando su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ubicación actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de actualización inválidos"),
            @ApiResponse(responseCode = "404", description = "La Ubicación, Especie o Distrito no fueron encontrados")
    })
    @PutMapping("/{idUbicacion}")
    public ResponseEntity<UbicacionEspecieResponseDTO> actualizarUbicacion(
            @PathVariable Long idUbicacion,
            @Valid @RequestBody UbicacionEspecieRequestDTO dto) {
        UbicacionEspecieResponseDTO response = ubicacionEspecieService.actualizarUbicacion(idUbicacion, dto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Desactivar ubicación (Borrado lógico)", description = "Desactiva la ubicación para no mostrarla, conservando el historial según requerimientos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Ubicación desactivada exitosamente (Sin contenido de respuesta)"),
            @ApiResponse(responseCode = "404", description = "Ubicación no encontrada")
    })
    @DeleteMapping("/{idUbicacion}")
    public ResponseEntity<Void> desactivarUbicacion(@PathVariable Long idUbicacion) {
        ubicacionEspecieService.desactivarUbicacion(idUbicacion);
        return ResponseEntity.noContent().build();
    }
}