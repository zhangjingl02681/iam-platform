package com.enterprise.iam.identity.web.dto;

import com.enterprise.iam.identity.domain.RoleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(
        @NotBlank @Size(min = 2, max = 64)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "may contain letters, digits, dot, underscore and hyphen only")
        String code,
        @NotBlank @Size(max = 128) String name,
        String description,
        RoleStatus status) {
}
