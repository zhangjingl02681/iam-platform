package com.enterprise.iam.pam.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePamAccountRequest(
        @NotBlank @Size(max = 128) String username, @NotBlank @Size(max = 1024) String secret, @Size(max = 64) String rotateCron) {}
