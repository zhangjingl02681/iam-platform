package com.enterprise.iam.policy.web.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * Decision is always evaluated for the currently authenticated user.
 */
public record PdpDecideRequest(
        @NotBlank String action,
        @NotBlank String resourceType,
        UUID resourceId,
        String resourceCode) {
}
