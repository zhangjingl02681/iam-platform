package com.enterprise.iam.identity.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignRoleRequest(@NotNull UUID roleId) {
}
