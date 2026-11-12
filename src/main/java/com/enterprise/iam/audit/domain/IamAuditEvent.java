package com.enterprise.iam.audit.domain;

import com.enterprise.iam.common.jpa.UuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "iam_audit_event")
public class IamAuditEvent extends UuidEntity {

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "event_type", nullable = false, length = 64, updatable = false)
    private String eventType;

    @Column(name = "actor_type", nullable = false, length = 32, updatable = false)
    private String actorType;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Column(name = "object_type", length = 64, updatable = false)
    private String objectType;

    @Column(name = "object_id", updatable = false)
    private UUID objectId;

    @Column(name = "action", nullable = false, length = 32, updatable = false)
    private String action;

    @Column(name = "result", nullable = false, length = 16, updatable = false)
    private String result;

    @Column(name = "ip", length = 64, updatable = false)
    private String ip;

    @Column(name = "user_agent", length = 512, updatable = false)
    private String userAgent;

    @Column(name = "request_id", length = 64, updatable = false)
    private String requestId;

    @Column(name = "session_id", updatable = false)
    private UUID sessionId;

    @Column(name = "client_id", length = 128, updatable = false)
    private String clientId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail", columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> detail;

    @Column(name = "prev_hash", length = 64, updatable = false)
    private String prevHash;

    @Column(name = "event_hash", nullable = false, length = 64, updatable = false)
    private String eventHash;

    @Override
    protected void onCreate() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public UUID getObjectId() {
        return objectId;
    }

    public void setObjectId(UUID objectId) {
        this.objectId = objectId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Map<String, Object> getDetail() {
        return detail;
    }

    public void setDetail(Map<String, Object> detail) {
        this.detail = detail;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public void setPrevHash(String prevHash) {
        this.prevHash = prevHash;
    }

    public String getEventHash() {
        return eventHash;
    }

    public void setEventHash(String eventHash) {
        this.eventHash = eventHash;
    }
}
