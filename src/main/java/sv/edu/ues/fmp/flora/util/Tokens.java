package sv.edu.ues.fmp.flora.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generacion y hash de tokens de un solo uso (por ejemplo, el enlace de
 * recuperacion de clave).
 * <p>
 * El token que recibe el usuario por correo (el valor "crudo") nunca se
 * guarda tal cual en la base de datos: se persiste su hash SHA-256, igual que
 * ya se hace con las claves en {@code clave_hash}. Aqui no hace falta BCrypt
 * como con las claves de usuario: el token no lo elige una persona sino
 * {@link SecureRandom}, con entropia suficiente para que un hash
 * determinista no sea atacable por diccionario. Quien solo tiene acceso a la
 * base de datos no puede reconstruir el enlace valido a partir del hash
 * guardado.
 */
public final class Tokens {

    private static final SecureRandom GENERADOR = new SecureRandom();
    private static final int LONGITUD_BYTES = 32;

    private Tokens() {
    }

    /** Valor aleatorio URL-safe que se envia por correo; no se persiste. */
    public static String generar() {
        byte[] bytes = new byte[LONGITUD_BYTES];
        GENERADOR.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Hash SHA-256 en hexadecimal; es lo unico que se guarda en la base de datos. */
    public static String hash(String tokenCrudo) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest(tokenCrudo.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no esta disponible en esta JVM", e);
        }
    }
}
