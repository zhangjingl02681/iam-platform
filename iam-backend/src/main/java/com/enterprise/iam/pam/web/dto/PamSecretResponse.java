package com.enterprise.iam.pam.web.dto;

import java.time.Instant;
import java.util.UUID;

public record PamSecretResponse(UUID checkoutId, UUID requestId, String username, String secret, Instant expiresAt) {}
