package io.mastermindarena.deduction.infrastructure.jdbc;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class RoomCredentials {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private RoomCredentials() {}

    public static String newSalt() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }

    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }

    public static String hashCode(String code, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(code.toCharArray(), Base64.getUrlDecoder().decode(salt), 120_000, 256);
            try {
                return ENCODER.encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded());
            } finally {
                spec.clearPassword();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash room code", e);
        }
    }

    public static boolean matchesCode(String code, String salt, String expectedHash) {
        if (code == null || salt == null || expectedHash == null) return false;
        return MessageDigest.isEqual(hashCode(code, salt).getBytes(StandardCharsets.US_ASCII),
                expectedHash.getBytes(StandardCharsets.US_ASCII));
    }

    public static String hashToken(String token) {
        try {
            return ENCODER.encodeToString(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash room token", e);
        }
    }

    public static boolean matchesToken(String token, String expectedHash) {
        if (token == null || expectedHash == null) return false;
        return MessageDigest.isEqual(hashToken(token).getBytes(StandardCharsets.US_ASCII),
                expectedHash.getBytes(StandardCharsets.US_ASCII));
    }
}