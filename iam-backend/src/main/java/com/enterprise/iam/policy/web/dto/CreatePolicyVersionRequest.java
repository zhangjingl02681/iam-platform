package com.enterprise.iam.policy.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePolicyVersionRequest(
        @NotBlank String document,
        String checksum,
        String language,
        String comment) {
}
