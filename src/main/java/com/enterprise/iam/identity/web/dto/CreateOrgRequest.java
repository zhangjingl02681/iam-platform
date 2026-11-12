package com.enterprise.iam.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateOrgRequest(
        UUID parentId,
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 128) String name,
        Integer sortOrder) {
}
