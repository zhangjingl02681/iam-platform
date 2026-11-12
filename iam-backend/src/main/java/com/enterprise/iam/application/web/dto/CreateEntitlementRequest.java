package com.enterprise.iam.application.web.dto;

import com.enterprise.iam.application.domain.EntitlementSubjectType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateEntitlementRequest(
        @NotNull EntitlementSubjectType subjectType,
        @NotNull UUID subjectId) {
}
