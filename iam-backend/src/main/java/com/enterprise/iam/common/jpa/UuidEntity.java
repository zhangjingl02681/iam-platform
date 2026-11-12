package com.enterprise.iam.common.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.util.Objects;
import java.util.UUID;

@MappedSuperclass
public abstract class UuidEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @PrePersist
    protected void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        onCreate();
    }

    @PreUpdate
    protected void preUpdate() {
        onUpdate();
    }

    protected void onCreate() {
    }

    protected void onUpdate() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UuidEntity other = (UuidEntity) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
