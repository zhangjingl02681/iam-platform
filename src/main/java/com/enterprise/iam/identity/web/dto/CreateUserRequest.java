package com.enterprise.iam.identity.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 64)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "may contain letters, digits, dot, underscore and hyphen only")
        String username,
        @Size(max = 64) String employeeNo,
        @NotBlank @Size(max = 128) String displayName,
        @Email @Size(max = 256) String email,
        @Size(max = 32) String mobile,
        UUID primaryOrgId,
        @Size(min = 12, max = 128) String password,
        List<AttributeValueRequest> attributes) {
}
