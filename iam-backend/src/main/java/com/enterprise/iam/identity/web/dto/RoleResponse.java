package com.enterprise.iam.identity.web.dto;

import com.enterprise.iam.identity.domain.IamRole;
import com.enterprise.iam.identity.domain.RoleStatus;
import java.time.Instant;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        String code,
        String name,
        String description,
        RoleStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static RoleResponse from(IamRole role) {
        return new RoleResponse(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.getStatus(),
                role.getCreatedAt(),
                role.getUpdatedAt());
    }
}
