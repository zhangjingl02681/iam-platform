package com.enterprise.iam.session.web.dto;

import com.enterprise.iam.session.domain.IamSession;
import com.enterprise.iam.session.domain.SessionStatus;
import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        UUID userId,
        String username,
        String ip,
        String userAgent,
        String amr,
        Instant createdAt,
        Instant lastSeenAt,
        Instant idleExpiresAt,
        Instant absExpiresAt,
        SessionStatus status,
        Instant revokedAt,
        boolean current) {

    public static SessionResponse from(IamSession session, String username, boolean current) {
        return new SessionResponse(
                session.getId(),
                session.getUserId(),
                username,
                session.getIp(),
                session.getUserAgent(),
                session.getAmr(),
                session.getCreatedAt(),
                session.getLastSeenAt(),
                session.getIdleExpiresAt(),
                session.getAbsExpiresAt(),
                session.getStatus(),
                session.getRevokedAt(),
                current);
    }
}
