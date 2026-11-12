package com.enterprise.iam.application.web.dto;

import com.enterprise.iam.application.domain.ApplicationStatus;
import com.enterprise.iam.application.domain.IamApplication;
import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        String code,
        String name,
        ApplicationStatus status,
        UUID ownerOrgId,
        String description,
        String launchUrl,
        String icon,
        String accentColor,
        Instant createdAt,
        Instant updatedAt) {

    public static ApplicationResponse from(IamApplication application) {
        return new ApplicationResponse(
                application.getId(),
                application.getCode(),
                application.getName(),
                application.getStatus(),
                application.getOwnerOrgId(),
                application.getDescription(),
                application.getLaunchUrl(),
                application.getIcon(),
                application.getAccentColor(),
                application.getCreatedAt(),
                application.getUpdatedAt());
    }
}
