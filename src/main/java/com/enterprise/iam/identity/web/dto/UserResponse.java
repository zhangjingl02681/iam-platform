package com.enterprise.iam.identity.web.dto;

import com.enterprise.iam.identity.domain.IamUser;
import com.enterprise.iam.identity.domain.IamUserAssignment;
import com.enterprise.iam.identity.domain.IamUserAttribute;
import com.enterprise.iam.identity.domain.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String employeeNo,
        String displayName,
        String email,
        String mobile,
        UserStatus status,
        UUID primaryOrgId,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt,
        List<AssignmentResponse> assignments,
        List<AttributeValueRequest> attributes) {

    public static UserResponse from(
            IamUser user,
            List<IamUserAssignment> assignments,
            List<IamUserAttribute> attributes) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmployeeNo(),
                user.getDisplayName(),
                user.getEmail(),
                user.getMobile(),
                user.getStatus(),
                user.getPrimaryOrgId(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                assignments.stream().map(AssignmentResponse::from).toList(),
                attributes.stream()
                        .map(attr -> new AttributeValueRequest(attr.getAttrCode(), attr.getValueText()))
                        .toList());
    }

    public record AssignmentResponse(
            UUID id,
            UUID orgId,
            UUID positionId,
            boolean primary,
            Instant validFrom,
            Instant validTo) {

        public static AssignmentResponse from(IamUserAssignment assignment) {
            return new AssignmentResponse(
                    assignment.getId(),
                    assignment.getOrgId(),
                    assignment.getPositionId(),
                    assignment.isPrimary(),
                    assignment.getValidFrom(),
                    assignment.getValidTo());
        }
    }
}
