package com.enterprise.iam.oidc.domain;

import com.enterprise.iam.common.jpa.UuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "iam_signing_key")
public class IamSigningKey extends UuidEntity {

    @Column(name = "kid", nullable = false, unique = true, length = 128)
    private String kid;

    @Column(name = "vault_ref", nullable = false, length = 256)
    private String vaultRef;

    @Column(name = "status", nullable = false, length = 16)
    private SigningKeyStatus status = SigningKeyStatus.CURRENT;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "retired_at")
    private Instant retiredAt;

    @Override
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    public String getVaultRef() {
        return vaultRef;
    }

    public void setVaultRef(String vaultRef) {
        this.vaultRef = vaultRef;
    }

    public SigningKeyStatus getStatus() {
        return status;
    }

    public void setStatus(SigningKeyStatus status) {
        this.status = status;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(Instant retiredAt) {
        this.retiredAt = retiredAt;
    }
}
