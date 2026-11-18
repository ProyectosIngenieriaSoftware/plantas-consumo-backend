package sv.edu.ues.fmp.flora.controller;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sv.edu.ues.fmp.flora.dto.request.EpocaCosechaActualizarRequest;
import sv.edu.ues.fmp.flora.dto.request.EpocaCosechaRequest;
import sv.edu.ues.fmp.flora.dto.response.EpocaCosechaResponse;
import sv.edu.ues.fmp.flora.service.EpocaCosechaService;

@RestController
@RequestMapping("/api/epocas-cosecha")
@RequiredArgsConstructor
public class EpocaCosechaController {

    private final EpocaCosechaService epocaCosechaService;

    /**
     * Las epocas de cosecha de una parte comestible. Abierto, como el resto de
     * la consulta del catalogo.
     *
     * <h2>Por que ya no pide ESPECIE_CONSULTAR</h2>
     * Entra por el id de la parte comestible, igual que
     * {@code GET /api/preparaciones-consumo/parte/{id}}, pero
     * --al contrario que sus hermanos-- nunca pregunto por el estado de la
     * ficha de la que cuelga esa parte, asi que sin permiso devolvia las epocas
     * de los borradores a quien probara numeros. Se cerro con
     * {@code ESPECIE_CONSULTAR} mientras le faltaba el filtro, y eso dejo al
     * catalogo publico sin un sub-recurso de las fichas PUBLICADAS: era el
     * unico listado de epocas, asi que un anonimo no podia pintar la epoca de
     * cosecha de una especie publicada.
     * <p>
     * Ahora {@code EpocaCosechaServiceImpl.listarPorParte} aplica
     * {@code VisibilidadDeEspecie} como sus hermanos: la epoca de una ficha
     * PUBLICADA se ve sin token, y la de una BORRADOR o EN_REVISION responde
     * 404 a quien no traiga {@code ESPECIE_CONSULTAR}. La decision vive en la
     * logica y no en la matriz por el 404: un 403 sobre el id de una parte
     * confirmaria que esta ocupado por una ficha sin publicar.
     */
    @GetMapping
    public ResponseEntity<List<EpocaCosechaResponse>> listar(
            @RequestParam Long idEspecieParte,
            @RequestParam(defaultValue = "false") boolean soloActivas
    ) {
        return ResponseEntity.ok(
                epocaCosechaService.listarPorParte(
                        idEspecieParte,
                        soloActivas
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EpocaCosechaResponse> obtenerPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                epocaCosechaService.obtenerPorId(id)
        );
    }

    @PreAuthorize("hasAuthority('COSECHA_GESTIONAR')")
    @PostMapping
    public ResponseEntity<EpocaCosechaResponse> crear(
            @Valid @RequestBody EpocaCosechaRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(epocaCosechaService.crear(request));
    }

    @PreAuthorize("hasAuthority('COSECHA_GESTIONAR')")
    @PutMapping("/{id}")
    public ResponseEntity<EpocaCosechaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EpocaCosechaActualizarRequest request
    ) {
        return ResponseEntity.ok(
                epocaCosechaService.actualizar(id, request)
        );
    }

    @PreAuthorize("hasAuthority('COSECHA_GESTIONAR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        epocaCosechaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('COSECHA_GESTIONAR')")
    @PatchMapping("/{id}/activar")
    public ResponseEntity<EpocaCosechaResponse> activar(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                epocaCosechaService.activar(id)
        );
    }
}
