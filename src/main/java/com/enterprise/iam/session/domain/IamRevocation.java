package com.enterprise.iam.session.domain;

import com.enterprise.iam.common.jpa.UuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "iam_revocation")
public class IamRevocation extends UuidEntity {

    @Column(name = "subject_type", nullable = false, length = 32)
    private String subjectType;

    @Column(name = "subject_value", nullable = false, length = 256)
    private String subjectValue;

    @Column(name = "reason", length = 256)
    private String reason;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    @Override
    protected void onCreate() {
        if (revokedAt == null) {
            revokedAt = Instant.now();
        }
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public String getSubjectValue() {
        return subjectValue;
    }

    public void setSubjectValue(String subjectValue) {
        this.subjectValue = subjectValue;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public UUID getRevokedBy() {
        return revokedBy;
    }

    public void setRevokedBy(UUID revokedBy) {
        this.revokedBy = revokedBy;
    }
}
