package com.enterprise.iam.pam.web.dto;

import com.enterprise.iam.pam.domain.PamLifecycleStatus;
import com.enterprise.iam.pam.domain.PamTarget;
import java.time.Instant;
import java.util.UUID;

public record PamTargetResponse(
        UUID id, String type, String name, String address, String env, PamLifecycleStatus status, Instant createdAt, Instant updatedAt) {

    public static PamTargetResponse from(PamTarget target) {
        return new PamTargetResponse(
                target.getId(),
                target.getType(),
                target.getName(),
                target.getAddress(),
                target.getEnv(),
                target.getStatus(),
                target.getCreatedAt(),
                target.getUpdatedAt());
    }
}
