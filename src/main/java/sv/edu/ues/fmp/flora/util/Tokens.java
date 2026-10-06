package sv.edu.ues.fmp.flora.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;


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
