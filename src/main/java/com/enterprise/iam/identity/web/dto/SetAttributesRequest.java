package com.enterprise.iam.identity.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SetAttributesRequest(
        @NotNull List<@Valid AttributeValueRequest> attributes) {
}
