package sv.edu.ues.fmp.flora.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Parte comestible de una especie: une una {@link Especie} con una entrada del
 * catalogo {@link PartePlanta} (por ejemplo "Inga paterno" + "Arilo").
 * Corresponde a la tabla {@code especie_parte_comestible}.
 * <p>
 * Es la tabla raiz del Lote 4: la referencian {@code preparacion_consumo},
 * {@code aporte_nutricional}, {@code epoca_cosecha}, {@code parte_beneficio},
 * {@code imagen_especie} y {@code video_especie}. Por eso:
 * <ul>
 *   <li>La baja es logica ({@code activa = false}), nunca un DELETE fisico.</li>
 *   <li>La parte asociada no cambia una vez creado el registro: cambiar
 *       "Arilo" por "Semilla" moveria en silencio todas las preparaciones,
 *       nutrientes y cosechas colgadas de este id a otra parte. Para otra parte
 *       se crea otro registro.</li>
 * </ul>
 * La bandera de estado de esta tabla esta en <strong>femenino</strong>
 * ({@code activa}), como {@code especie.activa} y a diferencia de
 * {@code parte_planta.activo}.
 * <p>
 * Restricciones que viven solo en la base y no se expresan en JPA:
 * <ul>
 *   <li>{@code uk_especie_parte}: UNIQUE ({@code id_especie},
 *       {@code id_parte_planta}). No es parcial: la combinacion es unica en todo
 *       el historial, activa o no. El servicio la anticipa antes de insertar
 *       para devolver un 409 especifico.</li>
 *   <li>{@code uk_especie_parte_id_especie}: UNIQUE ({@code id_especie_parte},
 *       {@code id_especie}). Es redundante como regla (la PK ya es unica);
 *       existe solo para que {@code imagen_especie} y {@code video_especie}
 *       (Lote 5) puedan declarar una FK compuesta que garantice que la parte
 *       referenciada pertenece a la misma especie que la imagen o el video. No
 *       requiere validacion en Java.</li>
 *   <li>{@code idx_parte_especie}: indice sobre {@code id_especie} que sirve a
 *       los listados por especie.</li>
 * </ul>
 * No se mapean colecciones inversas desde {@link Especie} ni desde
 * {@link PartePlanta}: los registros se recuperan por repositorio.
 */
@Entity
@Table(name = "especie_parte_comestible")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecieParteComestible {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_especie_parte", nullable = false, updatable = false)
    private Long idEspecieParte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_especie", nullable = false)
    private Especie especie;

    /** Inmutable por regla de negocio (ver Javadoc de la clase). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_parte_planta", nullable = false)
    private PartePlanta partePlanta;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;

    @Column(name = "advertencias", columnDefinition = "text")
    private String advertencias;

    @Builder.Default
    @Column(name = "activa", nullable = false)
    private Boolean activa = true;
}
