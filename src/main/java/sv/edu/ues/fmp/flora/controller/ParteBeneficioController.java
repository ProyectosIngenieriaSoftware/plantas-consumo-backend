package sv.edu.ues.fmp.flora.controller;

import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sv.edu.ues.fmp.flora.dto.request.ParteBeneficioRequest;
import sv.edu.ues.fmp.flora.dto.request.ParteBeneficioUpdateRequest;
import sv.edu.ues.fmp.flora.dto.response.ParteBeneficioResponse;
import sv.edu.ues.fmp.flora.service.ParteBeneficioService;

@RestController
@RequestMapping("/api/parte-beneficios")
@RequiredArgsConstructor
public class ParteBeneficioController {
    private final ParteBeneficioService parteBeneficioService;

    @GetMapping
    public ResponseEntity<List<ParteBeneficioResponse>> listarTodas() {
        return ResponseEntity.ok(parteBeneficioService.listarTodas());
    }

    @GetMapping("/activas")
    public ResponseEntity<List<ParteBeneficioResponse>> listarActivas() {
        return ResponseEntity.ok(parteBeneficioService.listarActivas());
    }

    @GetMapping("/{idEspecieParte}/{idBeneficio}")
    public ResponseEntity<ParteBeneficioResponse> obtenerPorId(@PathVariable Long idEspecieParte,
                                                               @PathVariable Long idBeneficio) {
        return ResponseEntity.ok(parteBeneficioService.obtenerPorId(idEspecieParte, idBeneficio));
    }

    @PostMapping
    public ResponseEntity<ParteBeneficioResponse> crear(@Valid @RequestBody ParteBeneficioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(parteBeneficioService.crear(request));
    }

    @PutMapping("/{idEspecieParte}/{idBeneficio}")
    public ResponseEntity<ParteBeneficioResponse> actualizar(@PathVariable Long idEspecieParte,
                                                             @PathVariable Long idBeneficio, @Valid @RequestBody ParteBeneficioUpdateRequest request) {
        return ResponseEntity.ok(parteBeneficioService.actualizar(idEspecieParte, idBeneficio, request));
    }

    @DeleteMapping("/{idEspecieParte}/{idBeneficio}")
    public ResponseEntity<Void> desactivar(@PathVariable Long idEspecieParte, @PathVariable Long idBeneficio) {
        parteBeneficioService.desactivar(idEspecieParte, idBeneficio);
        return ResponseEntity.noContent().build();
    }
}
