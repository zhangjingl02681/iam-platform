package com.enterprise.iam.identity.service;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * RFC 6238 TOTP (HMAC-SHA1, 30s, 6 digits). A {@link Clock} can be fixed in tests.
 */
@Component
public class TotpService {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int PERIOD_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final int WINDOW = 1;

    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public TotpService(Clock clock) {
        this.clock = clock;
    }

    public String generateSecret() {
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return base32Encode(bytes);
    }

    public String otpauthUri(String issuer, String account, String secret) {
        String label = url(issuer) + ":" + url(account);
        return "otpauth://totp/"
                + label
                + "?secret="
                + secret
                + "&issuer="
                + url(issuer)
                + "&algorithm=SHA1&digits="
                + CODE_DIGITS
                + "&period="
                + PERIOD_SECONDS;
    }

    public String currentCode(String secret) {
        return generate(secret, clock.instant());
    }

    public boolean verify(String secret, String code) {
        if (code == null) {
            return false;
        }
        String normalized = code.trim().replace(" ", "");
        if (!normalized.matches("\\d{6}")) {
            return false;
        }
        Instant now = clock.instant();
        for (int i = -WINDOW; i <= WINDOW; i++) {
            String expected = generate(secret, now.plusSeconds((long) i * PERIOD_SECONDS));
            if (constantTimeEquals(expected, normalized)) {
                return true;
            }
        }
        return false;
    }

    String generate(String secret, Instant instant) {
        long counter = Math.floorDiv(instant.getEpochSecond(), PERIOD_SECONDS);
        byte[] key = base32Decode(secret);
        byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            int otp = binary % 1_000_000;
            return String.format(Locale.ROOT, "%06d", otp);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate TOTP", ex);
        }
    }

    private static String url(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (left.length() != right.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length(); i++) {
            result |= left.charAt(i) ^ right.charAt(i);
        }
        return result == 0;
    }

    private static String base32Encode(byte[] data) {
        StringBuilder out = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                out.append(ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 0x1f));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            out.append(ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1f));
        }
        return out.toString();
    }

    private static byte[] base32Decode(String secret) {
        String normalized = secret.trim().replace(" ", "").replace("=", "").toUpperCase(Locale.ROOT);
        int buffer = 0;
        int bitsLeft = 0;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < normalized.length(); i++) {
            int val = ALPHABET.indexOf(normalized.charAt(i));
            if (val < 0) {
                throw new IllegalArgumentException("Invalid TOTP secret");
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.write((buffer >> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
            }
        }
        return out.toByteArray();
    }
}
