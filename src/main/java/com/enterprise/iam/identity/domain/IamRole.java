package com.enterprise.iam.identity.domain;

import com.enterprise.iam.common.jpa.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "iam_role")
public class IamRole extends AuditedEntity {

    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "status", nullable = false, length = 16)
    private RoleStatus status = RoleStatus.ACTIVE;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RoleStatus getStatus() {
        return status;
    }

    public void setStatus(RoleStatus status) {
        this.status = status;
    }
}
