package sv.edu.ues.fmp.flora.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sv.edu.ues.fmp.flora.dto.request.ImagenEspecieRequest;
import sv.edu.ues.fmp.flora.dto.request.ImagenPreparacionRequest;
import sv.edu.ues.fmp.flora.entity.enums.TipoImagen;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImagenRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void imagenEspecieRequest_cuandoUrlEsNull_soloFallaNotBlank_noGeneraClaveFantasmaUrlValida() {
        ImagenEspecieRequest request = ImagenEspecieRequest.builder()
                .tipoImagen(TipoImagen.PLANTA_COMPLETA)
                .textoAlternativo("Texto alternativo válido")
                .urlImagen(null)
                .build();

        Set<ConstraintViolation<ImagenEspecieRequest>> violations = validator.validate(request);

        // Debe fallar por @NotBlank en urlImagen
        boolean tieneNotBlankEnUrl = violations.stream().anyMatch(v ->
                "urlImagen".equals(v.getPropertyPath().toString())
                        && v.getMessage().contains("obligatoria"));
        assertTrue(tieneNotBlankEnUrl, "Debe reportar que la URL de la imagen es obligatoria");

        // NO debe existir la clave fantasma 'urlValida'
        boolean tieneUrlValida = violations.stream().anyMatch(v ->
                "urlValida".equals(v.getPropertyPath().toString()));
        assertFalse(tieneUrlValida, "No debe incluir el campo fantasma urlValida en la respuesta de validación");
    }

    @Test
    void imagenEspecieRequest_cuandoUrlEsInvalida_fallaUrlValida() {
        ImagenEspecieRequest request = ImagenEspecieRequest.builder()
                .tipoImagen(TipoImagen.PLANTA_COMPLETA)
                .textoAlternativo("Texto alternativo válido")
                .urlImagen("ftp://invalido.com/test.jpg")
                .build();

        Set<ConstraintViolation<ImagenEspecieRequest>> violations = validator.validate(request);

        boolean tieneUrlValida = violations.stream().anyMatch(v ->
                "urlValida".equals(v.getPropertyPath().toString()));
        assertTrue(tieneUrlValida, "Debe fallar urlValida cuando el formato no es http o https");
    }

    @Test
    void imagenPreparacionRequest_cuandoUrlEsNull_soloFallaNotBlank_noGeneraClaveFantasmaUrlValida() {
        ImagenPreparacionRequest request = ImagenPreparacionRequest.builder()
                .textoAlternativo("Texto alternativo válido")
                .urlImagen(null)
                .build();

        Set<ConstraintViolation<ImagenPreparacionRequest>> violations = validator.validate(request);

        // Debe fallar por @NotBlank en urlImagen
        boolean tieneNotBlankEnUrl = violations.stream().anyMatch(v ->
                "urlImagen".equals(v.getPropertyPath().toString())
                        && v.getMessage().contains("obligatoria"));
        assertTrue(tieneNotBlankEnUrl, "Debe reportar que la URL de la imagen es obligatoria");

        // NO debe existir la clave fantasma 'urlValida'
        boolean tieneUrlValida = violations.stream().anyMatch(v ->
                "urlValida".equals(v.getPropertyPath().toString()));
        assertFalse(tieneUrlValida, "No debe incluir el campo fantasma urlValida en la respuesta de validación");
    }

    @Test
    void imagenPreparacionRequest_cuandoUrlEsInvalida_fallaUrlValida() {
        ImagenPreparacionRequest request = ImagenPreparacionRequest.builder()
                .textoAlternativo("Texto alternativo válido")
                .urlImagen("ftp://invalido.com/test.jpg")
                .build();

        Set<ConstraintViolation<ImagenPreparacionRequest>> violations = validator.validate(request);

        boolean tieneUrlValida = violations.stream().anyMatch(v ->
                "urlValida".equals(v.getPropertyPath().toString()));
        assertTrue(tieneUrlValida, "Debe fallar urlValida cuando el formato no es http o https");
    }
}

