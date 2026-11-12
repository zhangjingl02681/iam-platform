package com.enterprise.iam.identity.web.dto;

import com.enterprise.iam.identity.domain.IamOrg;
import com.enterprise.iam.identity.domain.OrgStatus;
import java.time.Instant;
import java.util.UUID;

public record OrgResponse(
        UUID id,
        UUID parentId,
        String code,
        String name,
        String path,
        OrgStatus status,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt) {

    public static OrgResponse from(IamOrg org) {
        return new OrgResponse(
                org.getId(),
                org.getParentId(),
                org.getCode(),
                org.getName(),
                org.getPath(),
                org.getStatus(),
                org.getSortOrder(),
                org.getCreatedAt(),
                org.getUpdatedAt());
    }
}
