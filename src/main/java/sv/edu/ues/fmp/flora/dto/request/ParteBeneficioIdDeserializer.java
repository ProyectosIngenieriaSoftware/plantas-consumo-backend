package sv.edu.ues.fmp.flora.dto.request;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Lectura estricta de los IDs JSON de ParteBeneficio, sin configuracion global.
 * Los null quedan a cargo de @NotNull y la positividad a cargo de @Positive.
 */
public class ParteBeneficioIdDeserializer extends ValueDeserializer<Long> {
    @Override
    public Long deserialize(JsonParser parser, DeserializationContext context) {
        if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            return context.reportInputMismatch(Long.class,
                    "El identificador debe ser un número entero, sin comillas ni decimales");
        }
        try {
            return parser.getBigIntegerValue().longValueExact();
        } catch (ArithmeticException ex) {
            return context.reportInputMismatch(Long.class,
                    "El identificador está fuera del rango permitido para Long");
        }
    }
}
