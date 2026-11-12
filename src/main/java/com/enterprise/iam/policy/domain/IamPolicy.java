package com.enterprise.iam.policy.domain;

import com.enterprise.iam.common.jpa.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "iam_policy")
public class IamPolicy extends AuditedEntity {

    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "status", nullable = false, length = 16)
    private PolicyLifecycleStatus status = PolicyLifecycleStatus.DRAFT;

    @Column(name = "description")
    private String description;

    @Column(name = "created_by")
    private UUID createdBy;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PolicyLifecycleStatus getStatus() {
        return status;
    }

    public void setStatus(PolicyLifecycleStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
}
