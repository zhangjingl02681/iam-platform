package com.enterprise.iam.application.web.dto;

import com.enterprise.iam.application.domain.ApplicationStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateApplicationRequest(
        @Size(max = 128) String name,
        @Size(max = 2000) String description,
        @Size(max = 1024) String launchUrl,
        @Size(max = 64) String icon,
        @Size(max = 16)
        @Pattern(regexp = "^$|^#?[0-9A-Fa-f]{3,8}$", message = "must be a hex color")
        String accentColor,
        ApplicationStatus status,
        UUID ownerOrgId) {}
