package com.enterprise.iam.policy.domain;

import com.enterprise.iam.common.jpa.CreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "iam_policy_binding")
public class IamPolicyBinding extends CreatedAtEntity {

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Column(name = "version_id", nullable = false)
    private UUID versionId;

    @Column(name = "target_type", nullable = false, length = 32)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    public UUID getPolicyId() {
        return policyId;
    }

    public void setPolicyId(UUID policyId) {
        this.policyId = policyId;
    }

    public UUID getVersionId() {
        return versionId;
    }

    public void setVersionId(UUID versionId) {
        this.versionId = versionId;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public void setTargetId(UUID targetId) {
        this.targetId = targetId;
    }
}
