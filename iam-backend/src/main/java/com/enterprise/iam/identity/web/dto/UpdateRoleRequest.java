package com.enterprise.iam.identity.web.dto;

import com.enterprise.iam.identity.domain.RoleStatus;
import jakarta.validation.constraints.Size;

public record UpdateRoleRequest(
        @Size(max = 128) String name,
        String description,
        RoleStatus status) {
}
