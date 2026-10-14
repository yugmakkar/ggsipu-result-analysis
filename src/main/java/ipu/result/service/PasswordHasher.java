package ipu.result.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PBKDF2 password hashing (HMAC-SHA256). Stored form:
 *   iterations:saltBase64:hashBase64
 * No plaintext password is ever stored or logged. Verification is
 * constant-time to avoid leaking timing information.
 */
public class PasswordHasher {

    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256; // bits
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String hash(char[] password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] dk = pbkdf2(password, salt, ITERATIONS);
        return ITERATIONS + ":" + b64(salt) + ":" + b64(dk);
    }

    public static boolean verify(char[] password, String stored) {
        if (stored == null) return false;
        String[] parts = stored.split(":");
        if (parts.length != 3) return false;
        int iterations = Integer.parseInt(parts[0]);
        byte[] salt = unb64(parts[1]);
        byte[] expected = unb64(parts[2]);
        byte[] actual = pbkdf2(password, salt, iterations);
        return constantTimeEquals(expected, actual);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH);
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return f.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("password hashing failed", e);
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) diff |= a[i] ^ b[i];
        return diff == 0;
    }

    private static String b64(byte[] b) { return Base64.getEncoder().encodeToString(b); }
    private static byte[] unb64(String s) { return Base64.getDecoder().decode(s); }
}
