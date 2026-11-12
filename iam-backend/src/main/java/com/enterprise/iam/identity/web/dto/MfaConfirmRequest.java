package com.enterprise.iam.identity.web.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaConfirmRequest(@NotBlank String code) {}
