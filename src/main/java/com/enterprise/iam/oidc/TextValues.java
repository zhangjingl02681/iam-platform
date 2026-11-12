package com.enterprise.iam.oidc;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

final class TextValues {

    private TextValues() {
    }

    static Set<String> split(String value) {
        if (!StringUtils.hasText(value)) {
            return Set.of();
        }
        return Arrays.stream(value.split("[,\\s]+"))
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    static String join(Iterable<String> values) {
        if (values == null) {
            return null;
        }
        String joined = String.join(" ", values);
        return joined.isBlank() ? null : joined;
    }
}
