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
import sv.edu.ues.fmp.flora.dto.request.PartePlantaRequest;
import sv.edu.ues.fmp.flora.dto.response.PartePlantaResponse;
import sv.edu.ues.fmp.flora.service.PartePlantaService;

@RestController
@RequestMapping("/api/partes-planta")
@RequiredArgsConstructor
public class PartePlantaController {
    private final PartePlantaService partePlantaService;

    @GetMapping
    public ResponseEntity<List<PartePlantaResponse>> listar(
            @RequestParam(name = "soloActivas", defaultValue = "false") boolean soloActivas) {

        List<PartePlantaResponse> respuesta = soloActivas
                ? partePlantaService.listarActivas()
                : partePlantaService.listarTodas();

        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartePlantaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(partePlantaService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<PartePlantaResponse> crear(
            @Valid @RequestBody PartePlantaRequest request) {

        PartePlantaResponse creada = partePlantaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PartePlantaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PartePlantaRequest request) {

        return ResponseEntity.ok(partePlantaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        partePlantaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
