package com.enterprise.iam.identity.web.dto;

import com.enterprise.iam.identity.domain.IamOrg;
import com.enterprise.iam.identity.domain.OrgStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record OrgTreeNode(
        UUID id,
        UUID parentId,
        String code,
        String name,
        String path,
        OrgStatus status,
        int sortOrder,
        List<OrgTreeNode> children) {

    public static OrgTreeNode from(IamOrg org) {
        return new OrgTreeNode(
                org.getId(),
                org.getParentId(),
                org.getCode(),
                org.getName(),
                org.getPath(),
                org.getStatus(),
                org.getSortOrder(),
                new ArrayList<>());
    }
}
