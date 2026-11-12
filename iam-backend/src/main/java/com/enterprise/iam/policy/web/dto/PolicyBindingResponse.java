package com.enterprise.iam.policy.web.dto;

import com.enterprise.iam.policy.domain.IamPolicyBinding;
import java.time.Instant;
import java.util.UUID;

public record PolicyBindingResponse(
        UUID id,
        UUID policyId,
        UUID versionId,
        String targetType,
        UUID targetId,
        Instant createdAt) {

    public static PolicyBindingResponse from(IamPolicyBinding binding) {
        return new PolicyBindingResponse(
                binding.getId(),
                binding.getPolicyId(),
                binding.getVersionId(),
                binding.getTargetType(),
                binding.getTargetId(),
                binding.getCreatedAt());
    }
}
