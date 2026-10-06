package sv.edu.ues.fmp.flora.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sv.edu.ues.fmp.flora.entity.Taxonomia;

/**
 * Acceso a datos de {@link Taxonomia}.
 * <p>
 * A la taxonomia siempre se llega a traves de su especie, por eso no hay
 * metodos de listado: la unica consulta propia es la que anticipa la
 * restriccion {@code uk_taxonomia}.
 *
 * <h2>Por que esta consulta compara en minusculas</h2>
 * Usa {@code lower(...)}, mientras que {@code uk_taxonomia} compara los textos
 * tal cual. La divergencia es <strong>deliberada</strong>, no una
 * inconsistencia:
 * <ul>
 *   <li>La nomenclatura binomial exige genero con inicial mayuscula y epiteto
 *       en minuscula, asi que {@code Inga paterno} e {@code inga Paterno} no
 *       son dos clasificaciones distintas sino un error de captura.</li>
 *   <li>Es coherente con el resto del esquema, donde
 *       {@code especie.nombre_cientifico}, {@code usuario.correo} y
 *       {@code usuario.nombre_usuario} ya tienen indices unicos funcionales
 *       sobre {@code lower(...)}.</li>
 *   <li>{@code uk_taxonomia} es la excepcion probablemente porque
 *       {@code NULLS NOT DISTINCT} y un indice funcional sobre cuatro columnas
 *       no se combinan de forma directa en PostgreSQL.</li>
 * </ul>
 * Consecuencia practica: el servicio rechazara con 409 algunos casos que la
 * base habria admitido. Es el comportamiento buscado.
 */
@Repository
public interface TaxonomiaRepository extends JpaRepository<Taxonomia, Long> {

    /**
     * Busca una clasificacion identica, anticipando la restriccion
     * {@code uk_taxonomia UNIQUE NULLS NOT DISTINCT
     * (reino, genero, especie_taxonomica, subespecie)}.
     * <p>
     * Devuelve la entidad y no un {@code boolean} porque quien valida necesita
     * comparar el id: al actualizar, encontrar la taxonomia de la propia
     * especie que se esta editando no es un duplicado y hay que dejar guardar.
     * Esa exclusion la aplica {@code validarClasificacionUnica} en el servicio.
     * <p>
     * La doble condicion sobre {@code subespecie} replica en JPQL la semantica
     * de {@code NULLS NOT DISTINCT} (PostgreSQL 15+), que considera iguales dos
     * NULL. JPA no puede expresarla de otra forma: un metodo derivado como
     * {@code findBy...AndSubespecie} generaria {@code subespecie = null}, que
     * en SQL nunca es verdadero, de modo que dos taxonomias sin subespecie
     * jamas se detectarian como duplicadas.
     * <p>
     * El {@code CAST(:subespecie AS string)} no es decorativo. Sin el, al
     * comparar el parametro contra {@code IS NULL} Hibernate no puede inferir
     * su tipo y lo envia como {@code bytea}, el tipo binario generico de
     * PostgreSQL; el {@code lower(?)} de la rama siguiente falla entonces en
     * ejecucion con {@code function lower(bytea) does not exist}. El CAST
     * declara el tipo y Hibernate emite {@code cast(? as varchar)}.
     * Este fallo no lo detectan ni el compilador ni la validacion de arranque,
     * solo aparece al ejecutar con {@code subespecie} null; lo cubre
     * {@code TaxonomiaRepositoryTest}.
     * <p>
     * Las comparaciones van en {@code lower(...)}: ver la nota de la clase
     * sobre por que esta consulta es a proposito mas estricta que la
     * restriccion de la base.
     */
    @Query("""
        SELECT t FROM Taxonomia t
        WHERE lower(t.reino) = lower(:reino)
          AND lower(t.genero) = lower(:genero)
          AND lower(t.especieTaxonomica) = lower(:especie)
          AND ((CAST(:subespecie AS string) IS NULL AND t.subespecie IS NULL)
               OR lower(t.subespecie) = lower(CAST(:subespecie AS string)))
        """)
    Optional<Taxonomia> buscarPorClasificacion(@Param("reino") String reino,
                                               @Param("genero") String genero,
                                               @Param("especie") String especie,
                                               @Param("subespecie") String subespecie);
}
