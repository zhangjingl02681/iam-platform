package com.enterprise.iam.identity.domain;

import com.enterprise.iam.common.jpa.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "iam_credential")
public class IamCredential extends AuditedEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "type", nullable = false, length = 32)
    private CredentialType type = CredentialType.PASSWORD;

    @Column(name = "secret_hash", nullable = false, length = 255)
    private String secretHash;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "must_change", nullable = false)
    private boolean mustChange;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public CredentialType getType() {
        return type;
    }

    public void setType(CredentialType type) {
        this.type = type;
    }

    public String getSecretHash() {
        return secretHash;
    }

    public void setSecretHash(String secretHash) {
        this.secretHash = secretHash;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }

    public boolean isMustChange() {
        return mustChange;
    }

    public void setMustChange(boolean mustChange) {
        this.mustChange = mustChange;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }
}
