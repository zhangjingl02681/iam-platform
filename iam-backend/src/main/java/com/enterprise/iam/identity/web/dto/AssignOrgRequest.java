package com.enterprise.iam.identity.web.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record AssignOrgRequest(
        @NotNull UUID orgId,
        UUID positionId,
        Boolean primary,
        Instant validFrom,
        Instant validTo) {
}
