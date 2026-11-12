package com.enterprise.iam.oidc.web;

import com.enterprise.iam.oidc.domain.IamSigningKey;
import com.enterprise.iam.oidc.domain.SigningKeyStatus;
import java.time.Instant;

public record SigningKeyResponse(String kid, SigningKeyStatus status, Instant createdAt, Instant validUntil, Instant retiredAt) {

    public static SigningKeyResponse from(IamSigningKey key) {
        return new SigningKeyResponse(
                key.getKid(), key.getStatus(), key.getCreatedAt(), key.getValidUntil(), key.getRetiredAt());
    }
}
