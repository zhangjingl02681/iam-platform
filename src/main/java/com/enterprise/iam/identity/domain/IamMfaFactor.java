package com.enterprise.iam.identity.domain;

import com.enterprise.iam.common.jpa.UuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "iam_mfa_factor")
public class IamMfaFactor extends UuidEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @Column(name = "secret_enc")
    private String secretEnc;

    @Column(name = "label", length = 128)
    private String label;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "disabled_at")
    private Instant disabledAt;

    @Override
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSecretEnc() {
        return secretEnc;
    }

    public void setSecretEnc(String secretEnc) {
        this.secretEnc = secretEnc;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getDisabledAt() {
        return disabledAt;
    }

    public void setDisabledAt(Instant disabledAt) {
        this.disabledAt = disabledAt;
    }
}
