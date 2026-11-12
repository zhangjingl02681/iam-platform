package com.enterprise.iam.pam.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePamTargetRequest(
        @NotBlank @Size(max = 32) String type,
        @NotBlank @Size(max = 128) String name,
        @Size(max = 256) String address,
        @Size(max = 32) String env) {}
