package sv.edu.ues.fmp.flora.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sv.edu.ues.fmp.flora.dto.request.MunicipioRequest;
import sv.edu.ues.fmp.flora.dto.request.MunicipioResponse;
import sv.edu.ues.fmp.flora.service.MunicipioService;

import java.util.List;
@Tag(name = "Municipios de El Salvador", description = "Municipios registrados en el país")
@RestController
@RequestMapping("/api/municipios")
@RequiredArgsConstructor
public class MunicipioController {

    private final MunicipioService service;

    @GetMapping
    public ResponseEntity<List<MunicipioResponse>> listar(
            @RequestParam(required = false) Long idDepartamento) {

        return ResponseEntity.ok(idDepartamento != null
                ? service.listarPorDepartamento(idDepartamento)
                : service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MunicipioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<MunicipioResponse> crear(@Valid @RequestBody MunicipioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MunicipioResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody MunicipioRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        service.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}