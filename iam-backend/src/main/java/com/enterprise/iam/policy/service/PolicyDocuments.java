package com.enterprise.iam.policy.service;

import com.enterprise.iam.common.exception.BusinessException;
import com.enterprise.iam.policy.document.PolicyDocument;
import com.enterprise.iam.policy.document.PolicyResource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PolicyDocuments {

    public static final String ACTION_APP_LAUNCH = "app:launch";
    public static final String TARGET_APPLICATION = "application";
    public static final String TARGET_ORG = "org";

    private final JsonMapper jsonMapper = JsonMapper.shared();

    public PolicyDocuments() {
    }

    public PolicyDocument parse(String document) {
        if (!StringUtils.hasText(document)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid_policy_document", "Policy document is required");
        }
        PolicyDocument parsed;
        try {
            parsed = jsonMapper.readValue(document, PolicyDocument.class);
        } catch (JacksonException ex) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "invalid_policy_document", "Policy document is not valid JSON");
        }
        if (parsed == null || (!parsed.allow() && !parsed.deny())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "invalid_policy_document", "effect must be allow or deny");
        }
        if (parsed.actions() == null || parsed.actions().isEmpty()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "invalid_policy_document", "actions must not be empty");
        }
        if (parsed.resources() == null || parsed.resources().isEmpty()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "invalid_policy_document", "resources must not be empty");
        }
        for (PolicyResource resource : parsed.resources()) {
            if (resource == null || !StringUtils.hasText(resource.type())) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST, "invalid_policy_document", "each resource must have a type");
            }
        }
        return parsed;
    }

    public String checksum(String document) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(document.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    public static String normalizeTargetType(String targetType) {
        if (!StringUtils.hasText(targetType)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid_target_type", "Target type is required");
        }
        String normalized = targetType.trim().toLowerCase(Locale.ROOT);
        if (!TARGET_APPLICATION.equals(normalized) && !TARGET_ORG.equals(normalized)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "invalid_target_type", "Target type must be application or org");
        }
        return normalized;
    }
}
