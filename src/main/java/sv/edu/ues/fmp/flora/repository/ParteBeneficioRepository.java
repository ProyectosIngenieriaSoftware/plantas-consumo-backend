package sv.edu.ues.fmp.flora.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import sv.edu.ues.fmp.flora.entity.ParteBeneficio;
import sv.edu.ues.fmp.flora.entity.enums.ParteBeneficioId;

public interface ParteBeneficioRepository extends JpaRepository<ParteBeneficio, ParteBeneficioId> {

    List<ParteBeneficio> findByActivaTrue();

    /**
     * Los beneficios vigentes de una sola parte comestible, para el listado
     * por padre que pinta la ficha publica.
     * <p>
     * Pide <strong>dos</strong> banderas, y cada una tapa un agujero distinto:
     * <ul>
     *   <li>{@code activa = true} en la asociacion: el mismo criterio que
     *       {@link #findByActivaTrue()} y que
     *       {@code GET /api/parte-beneficios/activas}. Lo que se dio de baja
     *       es ese beneficio <em>para esa parte</em>.</li>
     *   <li>{@code beneficio.activo = true} en el catalogo: desde que
     *       {@code DELETE /api/beneficios/{id}} es una baja logica, la fila de
     *       {@code beneficio} sobrevive al borrado y sus asociaciones siguen
     *       activas --nadie las toca--. Sin esta segunda condicion, un
     *       beneficio retirado del catalogo seguiria saliendo en la ficha
     *       publica por esta puerta.</li>
     * </ul>
     * Los dos listados globales del modulo no llevan la segunda condicion a
     * proposito: son administrativos y exigen {@code ESPECIE_CONSULTAR}, asi
     * que ahi si conviene ver la asociacion de un beneficio retirado.
     * <p>
     * El orden por el id del beneficio es para que la respuesta no dependa del
     * que devuelva el motor.
     */
    List<ParteBeneficio>
    findByEspecieParteComestibleIdEspecieParteAndActivaTrueAndBeneficioActivoTrueOrderByIdIdBeneficioAsc(
            Long idEspecieParte
    );
}
