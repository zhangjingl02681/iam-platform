package com.enterprise.iam.application.domain;

import com.enterprise.iam.common.jpa.CreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(
        name = "iam_application_entitlement",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_iam_application_entitlement",
                columnNames = {"application_id", "subject_type", "subject_id"}))
public class IamApplicationEntitlement extends CreatedAtEntity {

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "subject_type", nullable = false, length = 16)
    private EntitlementSubjectType subjectType;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public EntitlementSubjectType getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(EntitlementSubjectType subjectType) {
        this.subjectType = subjectType;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(UUID subjectId) {
        this.subjectId = subjectId;
    }
}
