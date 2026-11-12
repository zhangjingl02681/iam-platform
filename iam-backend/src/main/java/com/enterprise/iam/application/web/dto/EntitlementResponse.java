package com.enterprise.iam.application.web.dto;

import com.enterprise.iam.application.domain.EntitlementSubjectType;
import com.enterprise.iam.application.domain.IamApplicationEntitlement;
import java.time.Instant;
import java.util.UUID;

public record EntitlementResponse(
        UUID id,
        UUID applicationId,
        EntitlementSubjectType subjectType,
        UUID subjectId,
        Instant createdAt) {

    public static EntitlementResponse from(IamApplicationEntitlement entitlement) {
        return new EntitlementResponse(
                entitlement.getId(),
                entitlement.getApplicationId(),
                entitlement.getSubjectType(),
                entitlement.getSubjectId(),
                entitlement.getCreatedAt());
    }
}
