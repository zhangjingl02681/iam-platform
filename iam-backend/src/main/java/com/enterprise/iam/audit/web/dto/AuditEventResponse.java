package com.enterprise.iam.audit.web.dto;

import com.enterprise.iam.audit.domain.IamAuditEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        Instant occurredAt,
        String eventType,
        String actorType,
        UUID actorId,
        String objectType,
        UUID objectId,
        String action,
        String result,
        String ip,
        String userAgent,
        String requestId,
        UUID sessionId,
        String clientId,
        Map<String, Object> detail,
        String prevHash,
        String eventHash) {

    public static AuditEventResponse from(IamAuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getOccurredAt(),
                event.getEventType(),
                event.getActorType(),
                event.getActorId(),
                event.getObjectType(),
                event.getObjectId(),
                event.getAction(),
                event.getResult(),
                event.getIp(),
                event.getUserAgent(),
                event.getRequestId(),
                event.getSessionId(),
                event.getClientId(),
                event.getDetail(),
                event.getPrevHash(),
                event.getEventHash());
    }
}
