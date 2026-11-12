package com.enterprise.iam.pam.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreatePamRequestRequest(
        @NotNull UUID accountId, @NotBlank String reason, @NotNull Instant startAt, @NotNull Instant endAt) {}
