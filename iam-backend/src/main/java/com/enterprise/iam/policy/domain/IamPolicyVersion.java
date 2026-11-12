package com.enterprise.iam.policy.domain;

import com.enterprise.iam.common.jpa.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "iam_policy_version")
public class IamPolicyVersion extends AuditedEntity {

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "language", length = 32)
    private String language;

    @Column(name = "document", nullable = false)
    private String document;

    @Column(name = "checksum", length = 128)
    private String checksum;

    @Column(name = "status", nullable = false, length = 16)
    private PolicyLifecycleStatus status = PolicyLifecycleStatus.DRAFT;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "published_by")
    private UUID publishedBy;

    @Column(name = "comment")
    private String comment;

    public UUID getPolicyId() {
        return policyId;
    }

    public void setPolicyId(UUID policyId) {
        this.policyId = policyId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public PolicyLifecycleStatus getStatus() {
        return status;
    }

    public void setStatus(PolicyLifecycleStatus status) {
        this.status = status;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public UUID getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(UUID publishedBy) {
        this.publishedBy = publishedBy;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
