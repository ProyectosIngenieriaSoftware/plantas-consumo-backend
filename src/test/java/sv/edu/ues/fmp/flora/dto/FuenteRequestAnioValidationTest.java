package sv.edu.ues.fmp.flora.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Year;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import sv.edu.ues.fmp.flora.dto.request.FuenteRequest;
import sv.edu.ues.fmp.flora.entity.enums.TipoFuente;

/**
 * El rango del anio de {@code fuente}, que el DTO y la base no compartian.
 *
 * <h2>Lo que estaba mal</h2>
 * {@code FuenteRequest.isAnioValido()} aceptaba desde el anio 1, pero la
 * columna tiene el CHECK {@code ck_fuente_anio}
 * --{@code anio IS NULL OR (anio >= 1000 AND anio <= 2100)}-- asi que los
 * anios de 1 a 999 pasaban la validacion y se estrellaban contra la base. Ese
 * {@code DataIntegrityViolationException} sale como un 409 con texto de base de
 * datos, cuando a un anio fuera de rango le toca un 400 de validacion con un
 * mensaje que se pueda leer. Lo reporto QA (Diego, punto 1: el formulario
 * acepta anios que la base rechaza).
 *
 * <h2>Por que las dos puntas se prueban distinto</h2>
 * El minimo (1000) viene del CHECK y es fijo. El maximo <strong>no</strong> se
 * copio del CHECK a proposito: la base admite hasta 2100, pero una fuente no se
 * publica en el futuro, asi que el DTO corta en el anio actual. Por eso el tope
 * se prueba con {@link Year#now()} y no con un literal: una prueba escrita
 * contra 2026 empezaria a fallar sola el 1 de enero.
 *
 * <h2>Cero residuo</h2>
 * Aqui no hay contexto de Spring ni base de datos: se valida el objeto con un
 * {@link Validator} de Jakarta, como
 * {@link UbicacionEspecieRequestValidationTest}. Que el 999 ya no llegue a la
 * base --el 400 en vez del 409-- lo comprueba
 * {@code FuenteAnioRangoTest} sobre el endpoint.
 */
class FuenteRequestAnioValidationTest {

    /**
     * El {@code @AssertTrue} cuelga de {@code isAnioValido()}, asi que la
     * violacion no se reporta en el campo {@code anio} sino en la propiedad
     * derivada {@code anioValido}. Es la clave que el cliente recibe dentro de
     * {@code erroresValidacion}.
     */
    private static final String PROPIEDAD = "anioValido";

    private static final String MENSAJE = "El año debe estar entre 1000 y el año actual";

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // ---------- El limite inferior: el que estaba desalineado ----------

    /** El caso de QA: 999 lo tiene que cortar la validacion, no el CHECK. */
    @Test
    void anio999_esRechazado() {
        Set<ConstraintViolation<FuenteRequest>> violaciones = validar((short) 999);

        assertTrue(falla(violaciones, PROPIEDAD),
                "999 queda por debajo del minimo del CHECK ck_fuente_anio (1000)");
        assertEquals(MENSAJE, mensajeDe(violaciones),
                "El mensaje debe decir el rango, para que el cliente sepa que corregir");
    }

    /** El primer anio que la base acepta tambien lo acepta el DTO. */
    @Test
    void anio1000_esAceptado() {
        assertTrue(validar((short) 1000).isEmpty(),
                "1000 es justo el minimo de ck_fuente_anio: tiene que pasar");
    }

    /**
     * Las dos puntas se tocan: 999 fuera y 1000 dentro. Si alguien moviera la
     * constante, una de las dos afirmaciones cae.
     */
    @Test
    void elCorteEstaExactamenteEntre999Y1000() {
        assertFalse(validar((short) 999).isEmpty(), "999 fuera");
        assertTrue(validar((short) 1000).isEmpty(), "1000 dentro");
    }

    // ---------- El limite superior: sigue siendo el anio actual ----------

    @Test
    void anioActual_esAceptado() {
        short actual = (short) Year.now().getValue();

        assertTrue(validar(actual).isEmpty(),
                "El anio en curso es el caso normal de una fuente recien publicada");
    }

    @Test
    void anioActualMasUno_esRechazado() {
        short futuro = (short) (Year.now().getValue() + 1);

        Set<ConstraintViolation<FuenteRequest>> violaciones = validar(futuro);

        assertTrue(falla(violaciones, PROPIEDAD),
                "Una fuente no se publica en el futuro, aunque la base admita hasta 2100");
        assertEquals(MENSAJE, mensajeDe(violaciones), "El mismo mensaje para las dos puntas");
    }

    // ---------- El campo sigue siendo opcional ----------

    @Test
    void anioNulo_esAceptado() {
        Set<ConstraintViolation<FuenteRequest>> violaciones = validar(null);

        assertFalse(falla(violaciones, PROPIEDAD),
                "El anio es opcional --la columna admite NULL-- y sin valor no debe fallar el rango");
        assertTrue(violaciones.isEmpty(), "Una fuente sin anio es valida");
    }

    // ---------- Armado ----------

    /** Una fuente valida en todo lo demas, para que solo el anio pueda fallar. */
    private static Set<ConstraintViolation<FuenteRequest>> validar(Short anio) {
        FuenteRequest request = FuenteRequest.builder()
                .titulo("Zzfuente para probar el rango del anio")
                .autor("Pruebas automáticas")
                // OTRO y no SITIO_WEB: SITIO_WEB exigiria url y meteria otra
                // violacion en el conjunto que estamos midiendo.
                .tipoFuente(TipoFuente.OTRO)
                .anio(anio)
                .build();

        return validator.validate(request);
    }

    private static boolean falla(Set<ConstraintViolation<FuenteRequest>> violaciones,
                                 String propiedad) {
        return violaciones.stream()
                .anyMatch(v -> propiedad.equals(v.getPropertyPath().toString()));
    }

    private static String mensajeDe(Set<ConstraintViolation<FuenteRequest>> violaciones) {
        return violaciones.stream()
                .filter(v -> PROPIEDAD.equals(v.getPropertyPath().toString()))
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse(null);
    }
}
