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
import sv.edu.ues.fmp.flora.dto.request.HabitatRequest;
import sv.edu.ues.fmp.flora.dto.response.HabitatResponse;
import sv.edu.ues.fmp.flora.service.HabitatService;

@RestController
@RequestMapping("/api/habitats")
@RequiredArgsConstructor
public class HabitatController {

    private final HabitatService habitatService;

    @GetMapping
    public ResponseEntity<List<HabitatResponse>> listar(
            @RequestParam(name = "soloActivos", defaultValue = "false") boolean soloActivos) {

        List<HabitatResponse> respuesta = soloActivos
                ? habitatService.listarActivos()
                : habitatService.listarTodos();

        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<HabitatResponse>> buscarPorNombre(
            @RequestParam(name = "nombre") String nombre) {

        return ResponseEntity.ok(habitatService.buscarPorNombre(nombre));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HabitatResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(habitatService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<HabitatResponse> crear(@Valid @RequestBody HabitatRequest request) {
        HabitatResponse creado = habitatService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HabitatResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody HabitatRequest request) {

        return ResponseEntity.ok(habitatService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        habitatService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}