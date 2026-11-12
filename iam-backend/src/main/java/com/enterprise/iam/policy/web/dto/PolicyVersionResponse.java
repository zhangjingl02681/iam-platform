package com.enterprise.iam.policy.web.dto;

import com.enterprise.iam.policy.domain.IamPolicyVersion;
import com.enterprise.iam.policy.domain.PolicyLifecycleStatus;
import java.time.Instant;
import java.util.UUID;

public record PolicyVersionResponse(
        UUID id,
        UUID policyId,
        int version,
        String language,
        String document,
        String checksum,
        PolicyLifecycleStatus status,
        Instant publishedAt,
        UUID publishedBy,
        String comment,
        Instant createdAt,
        Instant updatedAt) {

    public static PolicyVersionResponse from(IamPolicyVersion version) {
        return new PolicyVersionResponse(
                version.getId(),
                version.getPolicyId(),
                version.getVersion(),
                version.getLanguage(),
                version.getDocument(),
                version.getChecksum(),
                version.getStatus(),
                version.getPublishedAt(),
                version.getPublishedBy(),
                version.getComment(),
                version.getCreatedAt(),
                version.getUpdatedAt());
    }
}
