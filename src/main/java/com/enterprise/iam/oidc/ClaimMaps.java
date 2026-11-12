package com.enterprise.iam.oidc;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Restores token claim/metadata types after JSON (jsonb) round-trips, where
 * {@link Instant} values typically come back as numbers or ISO-8601 strings.
 */
final class ClaimMaps {

    private static final Set<String> INSTANT_CLAIMS = Set.of("exp", "iat", "nbf", "auth_time", "updated_at");

    private ClaimMaps() {
    }

    static Map<String, Object> restore(Map<String, Object> source) {
        if (source == null) {
            return new HashMap<>();
        }
        Map<String, Object> copy = HashMap.newHashMap(source.size());
        source.forEach((key, value) -> copy.put(key, restoreValue(key, value)));
        return copy;
    }

    private static Object restoreValue(String key, Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> nested = HashMap.newHashMap(map.size());
            map.forEach((nestedKey, nestedValue) -> {
                String name = String.valueOf(nestedKey);
                nested.put(name, restoreValue(name, nestedValue));
            });
            return nested;
        }
        if (INSTANT_CLAIMS.contains(key)) {
            Instant instant = toInstant(value);
            return instant != null ? instant : value;
        }
        return value;
    }

    private static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Number number) {
            double epoch = number.doubleValue();
            if (Math.abs(epoch) >= 10_000_000_000L) {
                return Instant.ofEpochMilli(number.longValue());
            }
            long seconds = (long) epoch;
            long nanos = Math.round((epoch - seconds) * 1_000_000_000L);
            return Instant.ofEpochSecond(seconds, nanos);
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Instant.parse(text);
            } catch (DateTimeParseException ex) {
                try {
                    return toInstant(Double.parseDouble(text));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}
