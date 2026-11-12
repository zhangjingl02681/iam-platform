package com.enterprise.iam.common.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;

@MappedSuperclass
public abstract class CreatedAtEntity extends UuidEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Override
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
