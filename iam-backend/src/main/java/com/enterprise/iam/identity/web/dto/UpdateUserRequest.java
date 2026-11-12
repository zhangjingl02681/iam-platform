package com.enterprise.iam.identity.web.dto;

import com.enterprise.iam.identity.domain.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateUserRequest(
        @Size(max = 128) String displayName,
        @Email @Size(max = 256) String email,
        @Size(max = 32) String mobile,
        @Size(max = 64) String employeeNo,
        UserStatus status,
        UUID primaryOrgId) {
}
