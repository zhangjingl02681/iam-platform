package com.enterprise.iam.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminSetPasswordRequest(
        @NotBlank @Size(min = 12, max = 128) String newPassword,
        Boolean mustChange) {
}
