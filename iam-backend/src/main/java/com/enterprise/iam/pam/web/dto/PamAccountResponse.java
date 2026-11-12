package com.enterprise.iam.pam.web.dto;

import com.enterprise.iam.pam.domain.PamAccount;
import com.enterprise.iam.pam.domain.PamLifecycleStatus;
import java.time.Instant;
import java.util.UUID;

public record PamAccountResponse(
        UUID id,
        UUID targetId,
        String username,
        String vaultRef,
        String rotateCron,
        PamLifecycleStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static PamAccountResponse from(PamAccount account) {
        return new PamAccountResponse(
                account.getId(),
                account.getTargetId(),
                account.getUsername(),
                account.getVaultRef(),
                account.getRotateCron(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }
}
