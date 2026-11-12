package com.enterprise.iam.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AttributeValueRequest(
        @NotBlank @Size(max = 64) String attrCode,
        String valueText) {
}
