package sv.edu.ues.fmp.flora.controller;

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
import sv.edu.ues.fmp.flora.dto.request.BeneficioRequest;
import sv.edu.ues.fmp.flora.dto.response.BeneficioResponse;
import sv.edu.ues.fmp.flora.service.BeneficioService;

import java.util.List;

@RestController
@RequestMapping("/api/beneficios")
@RequiredArgsConstructor
public class BeneficioController {

    private final BeneficioService beneficioService;

    /**
     * Listar los beneficios del catálogo.
     *
     * <p>Por omisión devuelve el catálogo completo, los dados de baja
     * incluidos: la pantalla de administración necesita verlos para poder
     * reactivarlos. Con {@code ?soloActivos=true} se omiten los que tienen
     * {@code activo = false}.</p>
     *
     * <p>Es la misma convención que {@code GET /api/habitats?soloActivos=true}
     * y {@code GET /api/partes-planta?soloActivas=true}. Un selector del
     * formulario debería pedir siempre {@code soloActivos=true}: desde que el
     * DELETE da de baja en lugar de borrar, sin la bandera también salen los
     * beneficios retirados.</p>
     */
    @GetMapping
    public ResponseEntity<List<BeneficioResponse>>
    listarTodos(
            @RequestParam(
                    name = "soloActivos",
                    defaultValue = "false"
            ) boolean soloActivos
    ) {

        List<BeneficioResponse> respuesta =
                soloActivos
                        ? beneficioService.listarActivos()
                        : beneficioService.listarTodos();

        return ResponseEntity.ok(respuesta);
    }

    /**
     * Buscar beneficio por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BeneficioResponse>
    obtenerPorId(@PathVariable Long id) {

        return ResponseEntity.ok(
                beneficioService.obtenerPorId(id)
        );
    }

    /**
     * Crear beneficio.
     */
    @PreAuthorize("hasAuthority('CATALOGO_GESTIONAR')")
    @PostMapping
    public ResponseEntity<BeneficioResponse>
    crear(
            @Valid
            @RequestBody BeneficioRequest request
    ) {

        BeneficioResponse beneficio =
                beneficioService.crear(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(beneficio);
    }

    /**
     * Actualizar beneficio.
     */
    @PreAuthorize("hasAuthority('CATALOGO_GESTIONAR')")
    @PutMapping("/{id}")
    public ResponseEntity<BeneficioResponse>
    actualizar(
            @PathVariable Long id,
            @Valid
            @RequestBody BeneficioRequest request
    ) {

        return ResponseEntity.ok(
                beneficioService.actualizar(
                        id,
                        request
                )
        );
    }

    /**
     * Dar de baja lógica un beneficio: responde 204 y la fila se queda con
     * {@code activo = false}.
     *
     * <p>Ya no borra la fila. Antes sí, y eso tenía dos consecuencias: un
     * beneficio citado por alguna ficha no se podía retirar --la FK de
     * {@code parte_beneficio} saltaba y salía un 409 de mensaje genérico-- y
     * cuando sí se borraba, las fichas perdían el dato. Ahora el beneficio en
     * uso se retira igual que cualquier otro: 204, la fila sigue ahí y las
     * asociaciones que la citan conservan su historia.</p>
     *
     * <p>Es el patrón del resto del catálogo: igual que
     * {@code DELETE /api/habitats/{id}} y {@code DELETE /api/partes-planta/{id}}.
     * Se revierte con {@link #activar(Long)}.</p>
     *
     * <p>Un beneficio dado de baja desaparece del catálogo público: no sale en
     * {@code GET /api/beneficios?soloActivos=true} ni en los beneficios de una
     * parte comestible ({@code GET /api/parte-beneficios/parte/{id}}), aunque
     * la asociación siga activa. Sigue visible en el listado completo y en
     * {@code GET /api/beneficios/{id}}, que es lo que permite reactivarlo.</p>
     */
    @PreAuthorize("hasAuthority('CATALOGO_GESTIONAR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void>
    desactivar(@PathVariable Long id) {

        beneficioService.desactivar(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivar un beneficio dado de baja: {@code activo = true}.
     *
     * <p>Idempotente: sobre uno que ya está activo no cambia nada y devuelve
     * 200 igualmente. Exige el mismo permiso que el DELETE,
     * {@code CATALOGO_GESTIONAR}; quien puede retirar un beneficio es quien
     * puede devolverlo.</p>
     */
    @PreAuthorize("hasAuthority('CATALOGO_GESTIONAR')")
    @PatchMapping("/{id}/activar")
    public ResponseEntity<BeneficioResponse>
    activar(@PathVariable Long id) {

        return ResponseEntity.ok(
                beneficioService.activar(id)
        );
    }
}
