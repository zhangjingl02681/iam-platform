package com.enterprise.iam.identity.web.dto;

import jakarta.validation.constraints.Size;

public record MfaEnrollRequest(@Size(max = 128) String label) {}
