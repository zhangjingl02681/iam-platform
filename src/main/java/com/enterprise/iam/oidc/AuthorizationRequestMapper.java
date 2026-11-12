package com.enterprise.iam.oidc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.StringUtils;

final class AuthorizationRequestMapper {

    private AuthorizationRequestMapper() {
    }

    static Map<String, Object> toMap(OAuth2AuthorizationRequest request) {
        if (request == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("authorizationUri", request.getAuthorizationUri());
        map.put("grantType", request.getGrantType().getValue());
        map.put("clientId", request.getClientId());
        map.put("redirectUri", request.getRedirectUri());
        map.put("state", request.getState());
        map.put("scopes", new ArrayList<>(request.getScopes()));
        map.put("additionalParameters", simpleMap(request.getAdditionalParameters()));
        map.put("attributes", simpleMap(request.getAttributes()));
        return map;
    }

    @SuppressWarnings("unchecked")
    static OAuth2AuthorizationRequest fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        var builder = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(string(map.get("authorizationUri")))
                .clientId(string(map.get("clientId")))
                .redirectUri(string(map.get("redirectUri")))
                .state(string(map.get("state")))
                .scopes(toStringSet(map.get("scopes")));
        if (map.get("additionalParameters") instanceof Map<?, ?> params) {
            builder.additionalParameters(simpleMap((Map<String, Object>) params));
        }
        if (map.get("attributes") instanceof Map<?, ?> attributes) {
            builder.attributes(attrs -> attrs.putAll(simpleMap((Map<String, Object>) attributes)));
        }
        if (map.get("grantType") instanceof String grant
                && !AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(grant)) {
            // authorizationCode() already sets the grant type used by this flow
        }
        return builder.build();
    }

    static Map<String, Object> simpleMap(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (source == null) {
            return copy;
        }
        source.forEach((key, value) -> {
            Object simple = simplify(value);
            if (simple != null) {
                copy.put(key, simple);
            }
        });
        return copy;
    }

    private static Object simplify(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(item -> String.valueOf(item)).collect(Collectors.toCollection(ArrayList::new));
        }
        return String.valueOf(value);
    }

    private static Set<String> toStringSet(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).collect(Collectors.toSet());
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            return TextValues.split(text);
        }
        return Set.of();
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
