package com.enterprise.iam.identity.domain;

import com.enterprise.iam.common.jpa.CreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "iam_password_history")
public class IamPasswordHistory extends CreatedAtEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "secret_hash", nullable = false, length = 255)
    private String secretHash;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getSecretHash() {
        return secretHash;
    }

    public void setSecretHash(String secretHash) {
        this.secretHash = secretHash;
    }
}
