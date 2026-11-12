package com.enterprise.iam.policy.web.dto;

import com.enterprise.iam.policy.domain.IamPolicy;
import com.enterprise.iam.policy.domain.PolicyLifecycleStatus;
import java.time.Instant;
import java.util.UUID;

public record PolicyResponse(
        UUID id,
        String code,
        String name,
        String description,
        PolicyLifecycleStatus status,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt) {

    public static PolicyResponse from(IamPolicy policy) {
        return new PolicyResponse(
                policy.getId(),
                policy.getCode(),
                policy.getName(),
                policy.getDescription(),
                policy.getStatus(),
                policy.getCreatedBy(),
                policy.getCreatedAt(),
                policy.getUpdatedAt());
    }
}
