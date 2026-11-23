package sv.edu.ues.fmp.flora.controller;

import java.util.List;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import sv.edu.ues.fmp.flora.dto.request.AporteNutricionalRequest;
import sv.edu.ues.fmp.flora.dto.response.AporteNutricionalResponse;
import sv.edu.ues.fmp.flora.service.AporteNutricionalService;

@RestController
@RequestMapping("/api/aportes-nutricionales")
@RequiredArgsConstructor
@Tag(name = "Aportes Nutricionales", description = "Gestión de los nutrientes aportados por una parte específica de la planta")
public class AporteNutricionalController {

    private final AporteNutricionalService aporteService;

    /**
     * Los aportes vigentes de una parte comestible: es el listado que pinta la
     * ficha publica.
     *
     * <p>Filtra por dos cosas, y las dos son obligatorias. La visibilidad del
     * padre, que ya tenia: la parte que no existe y la parte de una ficha que
     * quien llama no puede ver responden el mismo 404. Y, desde que el DELETE
     * da de baja en lugar de borrar, {@code activo = true}: un aporte retirado
     * no se pinta aqui. Esto <strong>no</strong> lleva bandera --no hay
     * {@code ?soloActivos} que lo apague-- porque es la garantia de que lo
     * retirado no vuelve al catalogo publico por descuido de un cliente.</p>
     *
     * <p>Una parte visible con todos sus aportes retirados devuelve 200 con
     * lista vacia, no 404: la parte existe y se puede ver.</p>
     */
    @Operation(summary = "Listar aportes vigentes por especie y parte",
            description = "Obtiene los nutrientes registrados para una parte específica de una planta. "
                    + "Omite los aportes dados de baja (activo = false) y responde 404 si la parte no existe "
                    + "o pertenece a una ficha que quien llama no puede ver.")
    @GetMapping("/especie-parte/{idEspecieParte}")
    public ResponseEntity<List<AporteNutricionalResponse>> listarPorEspecieParte(@PathVariable Long idEspecieParte) {
        return ResponseEntity.ok(aporteService.listarPorEspecieParte(idEspecieParte));
    }

    /**
     * Listado administrativo: el id de la URL es el del nutriente, no el de una
     * ficha, asi que la consulta cruza todas las especies y sin permiso
     * mostraria las partes de los borradores. El listado por especie-parte, que
     * si entra por el padre, sigue abierto.
     *
     * <p>Por ser el listado administrativo del modulo, es el que lleva la
     * bandera {@code ?soloActivos=true}. Por omision devuelve todo, los
     * aportes dados de baja incluidos: esta pantalla necesita verlos para
     * poder reactivarlos. Es la misma convencion que
     * {@code GET /api/beneficios?soloActivos=true},
     * {@code GET /api/habitats?soloActivos=true} y
     * {@code GET /api/partes-planta?soloActivas=true}; el valor por omision no
     * cambio con la baja logica, de modo que un cliente que ya llamaba a esta
     * ruta sigue recibiendo lo mismo.</p>
     *
     * <p>El reparto con el listado por parte no es casual y conviene no
     * invertirlo: aqui la bandera es una comodidad del administrador, mientras
     * que en {@code /especie-parte/{id}} el filtro de {@code activo} es
     * permanente, porque es el que de verdad protege la ficha publica.</p>
     */
    @Operation(summary = "Listar aportes por nutriente",
            description = "Requiere el permiso ESPECIE_CONSULTAR: cruza todas las especies, incluidas las que no estan publicadas. "
                    + "Obtiene todas las plantas/partes que aportan un nutriente específico. "
                    + "Por omisión incluye los aportes dados de baja; con ?soloActivos=true se omiten.")
    @PreAuthorize("hasAuthority('ESPECIE_CONSULTAR')")
    @GetMapping("/nutriente/{idNutriente}")
    public ResponseEntity<List<AporteNutricionalResponse>> listarPorNutriente(
            @PathVariable Long idNutriente,
            @RequestParam(name = "soloActivos", defaultValue = "false") boolean soloActivos) {

        List<AporteNutricionalResponse> respuesta = soloActivos
                ? aporteService.listarActivosPorNutriente(idNutriente)
                : aporteService.listarPorNutriente(idNutriente);

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Obtener un aporte nutricional por ID")
    @GetMapping("/{id}")
    public ResponseEntity<AporteNutricionalResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(aporteService.obtenerPorId(id));
    }

    @Operation(summary = "Registrar un nuevo aporte nutricional")
    @PreAuthorize("hasAuthority('NUTRICION_GESTIONAR')")
    @PostMapping
    public ResponseEntity<AporteNutricionalResponse> crear(
            @Valid @RequestBody AporteNutricionalRequest request) {
        AporteNutricionalResponse creada = aporteService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }


    @Operation(summary = "Actualizar un aporte nutricional existente")
    @PreAuthorize("hasAuthority('NUTRICION_GESTIONAR')")
    @PutMapping("/{id}")
    public ResponseEntity<AporteNutricionalResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody AporteNutricionalRequest request) {
        return ResponseEntity.ok(aporteService.actualizar(id, request));
    }

    /**
     * Dar de baja logica un aporte nutricional: responde 204 y la fila se
     * queda con {@code activo = false}.
     *
     * <p>Ya no borra la fila. Antes si, y eso tenia dos consecuencias: se
     * perdia una medicion citada a una fuente --un dato que alguien levanto y
     * referencio-- y un borrado por error solo se arreglaba volviendo a
     * teclear la cantidad, la unidad y la porcion de referencia. Ahora el
     * aporte se retira igual que cualquier otra pieza del catalogo: 204, la
     * fila sigue ahi y el historial se conserva.</p>
     *
     * <p>Es el patron del resto del catalogo: igual que
     * {@code DELETE /api/beneficios/{id}}, {@code DELETE /api/habitats/{id}} y
     * {@code DELETE /api/partes-planta/{id}}. Se revierte con
     * {@link #activar(Long)}.</p>
     *
     * <p>Un aporte dado de baja desaparece de la ficha publica --no sale en
     * {@code GET /api/aportes-nutricionales/especie-parte/{id}}-- ni en
     * {@code GET /api/aportes-nutricionales/nutriente/{id}?soloActivos=true}.
     * Sigue visible en el listado completo por nutriente y en
     * {@code GET /api/aportes-nutricionales/{id}}, con su bandera en
     * {@code false}, que es lo que permite reactivarlo; el GET posterior al
     * DELETE ya no es un 404.</p>
     */
    @Operation(summary = "Dar de baja un aporte nutricional",
            description = "Baja lógica: deja la fila con activo = false y responde 204. No borra el registro; "
                    + "se revierte con PATCH /api/aportes-nutricionales/{id}/activar.")
    @PreAuthorize("hasAuthority('NUTRICION_GESTIONAR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        aporteService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivar un aporte nutricional dado de baja: {@code activo = true}.
     *
     * <p>Idempotente: sobre uno que ya esta activo no cambia nada y devuelve
     * 200 igualmente. Un id que no existe sigue siendo 404.</p>
     *
     * <p>Exige el mismo permiso que el DELETE, {@code NUTRICION_GESTIONAR};
     * quien puede retirar un aporte es quien puede devolverlo. No hizo falta
     * tocar {@code SecurityConfig}: {@code CATALOGO_PUBLICO} abre
     * {@code /api/aportes-nutricionales/**} solo para {@code GET}, asi que
     * este PATCH cae en el {@code anyRequest().authenticated()} --401 al
     * anonimo-- y el 403 de quien trae token sin el permiso lo pone este
     * {@code @PreAuthorize}.</p>
     */
    @Operation(summary = "Reactivar un aporte nutricional dado de baja",
            description = "Pone activo = true y responde 200 con el aporte. Idempotente: sobre uno ya activo "
                    + "no cambia nada. Mismo permiso que el DELETE.")
    @PreAuthorize("hasAuthority('NUTRICION_GESTIONAR')")
    @PatchMapping("/{id}/activar")
    public ResponseEntity<AporteNutricionalResponse> activar(@PathVariable Long id) {
        return ResponseEntity.ok(aporteService.activar(id));
    }
}
