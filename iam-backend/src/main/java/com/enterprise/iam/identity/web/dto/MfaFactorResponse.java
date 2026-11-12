package com.enterprise.iam.identity.web.dto;

import com.enterprise.iam.identity.domain.IamMfaFactor;
import java.time.Instant;
import java.util.UUID;

public record MfaFactorResponse(
        UUID id,
        String type,
        String label,
        Instant verifiedAt,
        Instant createdAt,
        Instant disabledAt) {

    public static MfaFactorResponse from(IamMfaFactor factor) {
        return new MfaFactorResponse(
                factor.getId(),
                factor.getType(),
                factor.getLabel(),
                factor.getVerifiedAt(),
                factor.getCreatedAt(),
                factor.getDisabledAt());
    }
}
