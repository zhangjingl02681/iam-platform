package com.enterprise.iam.pam.domain;

import com.enterprise.iam.common.jpa.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "pam_account")
public class PamAccount extends AuditedEntity {

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "username", nullable = false, length = 128)
    private String username;

    @Column(name = "vault_ref", length = 256)
    private String vaultRef;

    @Column(name = "rotate_cron", length = 64)
    private String rotateCron;

    @Column(name = "status", nullable = false, length = 16)
    private PamLifecycleStatus status = PamLifecycleStatus.ACTIVE;

    public UUID getTargetId() {
        return targetId;
    }

    public void setTargetId(UUID targetId) {
        this.targetId = targetId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getVaultRef() {
        return vaultRef;
    }

    public void setVaultRef(String vaultRef) {
        this.vaultRef = vaultRef;
    }

    public String getRotateCron() {
        return rotateCron;
    }

    public void setRotateCron(String rotateCron) {
        this.rotateCron = rotateCron;
    }

    public PamLifecycleStatus getStatus() {
        return status;
    }

    public void setStatus(PamLifecycleStatus status) {
        this.status = status;
    }
}
