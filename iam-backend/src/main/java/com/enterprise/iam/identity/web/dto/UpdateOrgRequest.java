package com.enterprise.iam.identity.web.dto;

import com.enterprise.iam.identity.domain.OrgStatus;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateOrgRequest(
        @Size(max = 128) String name,
        OrgStatus status,
        Integer sortOrder,
        UUID parentId,
        boolean clearParent) {
}
