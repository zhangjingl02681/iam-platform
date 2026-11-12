package com.enterprise.iam.application.domain;

import com.enterprise.iam.common.jpa.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "iam_application")
public class IamApplication extends AuditedEntity {

    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "status", nullable = false, length = 16)
    private ApplicationStatus status = ApplicationStatus.ACTIVE;

    @Column(name = "owner_org_id")
    private UUID ownerOrgId;

    @Column(name = "description")
    private String description;

    @Column(name = "launch_url", length = 1024)
    private String launchUrl;

    @Column(name = "icon", length = 64)
    private String icon;

    @Column(name = "accent_color", length = 16)
    private String accentColor;

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

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public UUID getOwnerOrgId() {
        return ownerOrgId;
    }

    public void setOwnerOrgId(UUID ownerOrgId) {
        this.ownerOrgId = ownerOrgId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLaunchUrl() {
        return launchUrl;
    }

    public void setLaunchUrl(String launchUrl) {
        this.launchUrl = launchUrl;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
    }
}
