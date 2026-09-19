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
import sv.edu.ues.fmp.flora.dto.request.FuenteRequest;
import sv.edu.ues.fmp.flora.dto.response.FuenteResponse;
import sv.edu.ues.fmp.flora.service.FuenteService;

@RestController
@RequestMapping("/api/fuentes")
@RequiredArgsConstructor
public class FuenteController {
    private final FuenteService fuenteService;

    @GetMapping
    public ResponseEntity<List<FuenteResponse>> listar(
            @RequestParam(name = "soloActivas", defaultValue = "false") boolean soloActivas) {

        List<FuenteResponse> respuesta = soloActivas
                ? fuenteService.listarActivas()
                : fuenteService.listarTodas();

        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FuenteResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(fuenteService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<FuenteResponse> crear(
            @Valid @RequestBody FuenteRequest request) {

        FuenteResponse creada = fuenteService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FuenteResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody FuenteRequest request) {

        return ResponseEntity.ok(fuenteService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        fuenteService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
