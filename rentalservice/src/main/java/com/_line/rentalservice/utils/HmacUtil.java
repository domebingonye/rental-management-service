package com._line.rentalservice.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/** HMAC-SHA512 signing for webhooks (same scheme Paystack uses for x-paystack-signature). */
public final class HmacUtil {
    private static final String ALGORITHM = "HmacSHA512";

    private HmacUtil() {}

    public static String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to compute HMAC", e);
        }
    }

    /** Constant-time comparison so the signature cannot be guessed byte by byte from response timings. */
    public static boolean verify(String payload, String signature, String secret) {
        if (payload == null || signature == null || signature.isBlank()) return false;
        byte[] expected = sign(payload, secret).getBytes(StandardCharsets.UTF_8);
        byte[] actual = signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    /** Constant-time equality for API keys. */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
