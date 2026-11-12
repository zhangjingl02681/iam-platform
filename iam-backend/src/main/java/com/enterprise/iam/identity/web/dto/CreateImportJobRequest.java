package com.enterprise.iam.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateImportJobRequest(
        @NotBlank @Size(max = 32) String type,
        String fileRef,
        String csv) {
}
