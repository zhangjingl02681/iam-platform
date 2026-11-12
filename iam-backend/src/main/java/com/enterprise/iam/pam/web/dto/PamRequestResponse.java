package com.enterprise.iam.pam.web.dto;

import com.enterprise.iam.pam.domain.PamRequest;
import com.enterprise.iam.pam.domain.PamRequestStatus;
import java.time.Instant;
import java.util.UUID;

public record PamRequestResponse(
        UUID id,
        UUID accountId,
        UUID requesterId,
        String reason,
        Instant startAt,
        Instant endAt,
        PamRequestStatus status,
        UUID approverId,
        Instant decidedAt,
        boolean breakGlass,
        Instant createdAt,
        Instant updatedAt) {

    public static PamRequestResponse from(PamRequest request) {
        return new PamRequestResponse(
                request.getId(),
                request.getAccountId(),
                request.getRequesterId(),
                request.getReason(),
                request.getStartAt(),
                request.getEndAt(),
                request.getStatus(),
                request.getApproverId(),
                request.getDecidedAt(),
                request.isBreakGlass(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
