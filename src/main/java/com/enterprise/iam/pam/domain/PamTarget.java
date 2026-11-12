package com.enterprise.iam.pam.domain;

import com.enterprise.iam.common.jpa.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "pam_target")
public class PamTarget extends AuditedEntity {

    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "address", length = 256)
    private String address;

    @Column(name = "env", length = 32)
    private String env;

    @Column(name = "status", nullable = false, length = 16)
    private PamLifecycleStatus status = PamLifecycleStatus.ACTIVE;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public PamLifecycleStatus getStatus() {
        return status;
    }

    public void setStatus(PamLifecycleStatus status) {
        this.status = status;
    }
}
