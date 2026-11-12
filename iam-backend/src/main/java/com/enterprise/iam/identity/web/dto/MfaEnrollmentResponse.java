package com.enterprise.iam.identity.web.dto;

import java.time.Instant;
import java.util.UUID;

public record MfaEnrollmentResponse(
        UUID id, String type, String label, Instant verifiedAt, String secret, String otpauthUri) {}
