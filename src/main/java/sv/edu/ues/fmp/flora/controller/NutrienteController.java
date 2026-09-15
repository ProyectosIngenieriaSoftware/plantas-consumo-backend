package sv.edu.ues.fmp.flora.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.dto.request.NutrienteRequest;
import sv.edu.ues.fmp.flora.dto.response.NutrienteResponse;
import sv.edu.ues.fmp.flora.entity.enums.CategoriaNutriente;
import sv.edu.ues.fmp.flora.service.NutrienteService;

@RestController
@RequestMapping("/api/nutrientes")
@RequiredArgsConstructor
public class NutrienteController {

    private final NutrienteService nutrienteService;

    @GetMapping
    public ResponseEntity<List<NutrienteResponse>> listar(
            @RequestParam(name = "soloActivas", defaultValue = "false") boolean soloActivas) {

        List<NutrienteResponse> respuesta = soloActivas
                ? nutrienteService.listarActivas()
                : nutrienteService.listarTodas();

        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NutrienteResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(nutrienteService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<NutrienteResponse> crear(
            @Valid @RequestBody NutrienteRequest request) {
        NutrienteResponse creada = nutrienteService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NutrienteResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody NutrienteRequest request) {
        return ResponseEntity.ok(nutrienteService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        nutrienteService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
    // NUEVO: Endpoint para buscar por coincidencia de texto
    // Ejemplo de uso: GET /api/nutrientes/buscar?nombre=vitam
    @GetMapping("/buscar")
    public ResponseEntity<List<NutrienteResponse>> buscarPorNombre(
            @RequestParam(name = "nombre") String palabraClave) {
        return ResponseEntity.ok(nutrienteService.buscarPorNombre(palabraClave));
    }

    // NUEVO: Endpoint para filtrar por categoría
    // Ejemplo de uso: GET /api/nutrientes/categoria/VITAMINA
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<NutrienteResponse>> buscarPorCategoria(
            @PathVariable CategoriaNutriente categoria) {
        return ResponseEntity.ok(nutrienteService.buscarPorCategoria(categoria));
    }
}