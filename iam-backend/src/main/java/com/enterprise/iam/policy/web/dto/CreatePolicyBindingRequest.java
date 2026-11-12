package com.enterprise.iam.policy.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreatePolicyBindingRequest(
        @NotNull UUID versionId,
        @NotBlank String targetType,
        @NotNull UUID targetId) {
}
