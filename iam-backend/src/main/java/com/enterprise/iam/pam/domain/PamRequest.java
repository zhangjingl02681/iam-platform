package com.enterprise.iam.pam.domain;

import com.enterprise.iam.common.jpa.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pam_request")
public class PamRequest extends AuditedEntity {

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "reason")
    private String reason;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "status", nullable = false, length = 16)
    private PamRequestStatus status = PamRequestStatus.PENDING;

    @Column(name = "approver_id")
    private UUID approverId;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "break_glass", nullable = false)
    private boolean breakGlass;

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(UUID requesterId) {
        this.requesterId = requesterId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    public PamRequestStatus getStatus() {
        return status;
    }

    public void setStatus(PamRequestStatus status) {
        this.status = status;
    }

    public UUID getApproverId() {
        return approverId;
    }

    public void setApproverId(UUID approverId) {
        this.approverId = approverId;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public boolean isBreakGlass() {
        return breakGlass;
    }

    public void setBreakGlass(boolean breakGlass) {
        this.breakGlass = breakGlass;
    }
}
