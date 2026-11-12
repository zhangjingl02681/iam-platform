package com.enterprise.iam.application.service;

import com.enterprise.iam.application.domain.EntitlementSubjectType;
import com.enterprise.iam.application.domain.IamApplication;
import com.enterprise.iam.application.domain.IamApplicationEntitlement;
import com.enterprise.iam.application.repo.IamApplicationEntitlementRepository;
import com.enterprise.iam.application.repo.IamApplicationRepository;
import com.enterprise.iam.application.web.dto.CreateEntitlementRequest;
import com.enterprise.iam.application.web.dto.EntitlementResponse;
import com.enterprise.iam.audit.AuditService;
import com.enterprise.iam.common.exception.ConflictException;
import com.enterprise.iam.common.exception.NotFoundException;
import com.enterprise.iam.identity.domain.IamUserAssignment;
import com.enterprise.iam.identity.repo.IamOrgRepository;
import com.enterprise.iam.identity.repo.IamRoleRepository;
import com.enterprise.iam.identity.repo.IamUserAssignmentRepository;
import com.enterprise.iam.identity.repo.IamUserRepository;
import com.enterprise.iam.identity.repo.IamUserRoleRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationEntitlementService {

    private final IamApplicationRepository applicationRepository;
    private final IamApplicationEntitlementRepository entitlementRepository;
    private final IamUserRepository userRepository;
    private final IamOrgRepository orgRepository;
    private final IamRoleRepository roleRepository;
    private final IamUserAssignmentRepository assignmentRepository;
    private final IamUserRoleRepository userRoleRepository;
    private final AuditService auditService;

    public ApplicationEntitlementService(
            IamApplicationRepository applicationRepository,
            IamApplicationEntitlementRepository entitlementRepository,
            IamUserRepository userRepository,
            IamOrgRepository orgRepository,
            IamRoleRepository roleRepository,
            IamUserAssignmentRepository assignmentRepository,
            IamUserRoleRepository userRoleRepository,
            AuditService auditService) {
        this.applicationRepository = applicationRepository;
        this.entitlementRepository = entitlementRepository;
        this.userRepository = userRepository;
        this.orgRepository = orgRepository;
        this.roleRepository = roleRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRoleRepository = userRoleRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<EntitlementResponse> list(UUID applicationId) {
        requireApplication(applicationId);
        return entitlementRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId).stream()
                .map(EntitlementResponse::from)
                .toList();
    }

    @Transactional
    public EntitlementResponse add(UUID applicationId, CreateEntitlementRequest request) {
        requireApplication(applicationId);
        validateSubject(request.subjectType(), request.subjectId());
        if (entitlementRepository.existsByApplicationIdAndSubjectTypeAndSubjectId(
                applicationId, request.subjectType(), request.subjectId())) {
            throw new ConflictException("entitlement_exists", "This entitlement already exists");
        }
        IamApplicationEntitlement entitlement = new IamApplicationEntitlement();
        entitlement.setApplicationId(applicationId);
        entitlement.setSubjectType(request.subjectType());
        entitlement.setSubjectId(request.subjectId());
        entitlement = entitlementRepository.save(entitlement);
        auditService.record(
                "application.entitlement.add",
                "create",
                "application",
                applicationId,
                "success",
                Map.of(
                        "entitlementId", entitlement.getId().toString(),
                        "subjectType", request.subjectType().name(),
                        "subjectId", request.subjectId().toString()));
        return EntitlementResponse.from(entitlement);
    }

    @Transactional
    public void remove(UUID applicationId, UUID entitlementId) {
        requireApplication(applicationId);
        IamApplicationEntitlement entitlement = entitlementRepository
                .findByApplicationIdAndId(applicationId, entitlementId)
                .orElseThrow(() -> new NotFoundException("entitlement_not_found", "Entitlement not found"));
        entitlementRepository.delete(entitlement);
        auditService.record(
                "application.entitlement.remove",
                "delete",
                "application",
                applicationId,
                "success",
                Map.of(
                        "entitlementId", entitlementId.toString(),
                        "subjectType", entitlement.getSubjectType().name(),
                        "subjectId", entitlement.getSubjectId().toString()));
    }

    @Transactional(readOnly = true)
    public boolean isVisibleTo(UUID userId, UUID applicationId) {
        List<IamApplicationEntitlement> entitlements =
                entitlementRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId);
        if (entitlements.isEmpty()) {
            return true;
        }
        SubjectContext context = subjectContext(userId);
        return entitlements.stream().anyMatch(item -> matches(item, context));
    }

    @Transactional(readOnly = true)
    public Set<UUID> visibleApplicationIds(UUID userId, List<IamApplication> applications) {
        List<UUID> ids = applications.stream().map(IamApplication::getId).toList();
        Map<UUID, List<IamApplicationEntitlement>> byApp = entitlementRepository.findByApplicationIdIn(ids).stream()
                .collect(Collectors.groupingBy(IamApplicationEntitlement::getApplicationId));
        SubjectContext context = subjectContext(userId);
        Set<UUID> visible = new HashSet<>();
        for (IamApplication application : applications) {
            List<IamApplicationEntitlement> entitlements = byApp.getOrDefault(application.getId(), List.of());
            if (entitlements.isEmpty() || entitlements.stream().anyMatch(item -> matches(item, context))) {
                visible.add(application.getId());
            }
        }
        return visible;
    }

    private boolean matches(IamApplicationEntitlement entitlement, SubjectContext context) {
        return switch (entitlement.getSubjectType()) {
            case USER -> context.userId.equals(entitlement.getSubjectId());
            case ORG -> context.orgIds.contains(entitlement.getSubjectId());
            case ROLE -> context.roleIds.contains(entitlement.getSubjectId());
        };
    }

    private SubjectContext subjectContext(UUID userId) {
        Set<UUID> orgIds = assignmentRepository.findByUserId(userId).stream()
                .map(IamUserAssignment::getOrgId)
                .collect(Collectors.toSet());
        Set<UUID> roleIds = userRoleRepository.findByUserId(userId).stream()
                .map(item -> item.getRoleId())
                .collect(Collectors.toSet());
        return new SubjectContext(userId, orgIds, roleIds);
    }

    private void validateSubject(EntitlementSubjectType type, UUID subjectId) {
        boolean exists = switch (type) {
            case USER -> userRepository.existsById(subjectId);
            case ORG -> orgRepository.existsById(subjectId);
            case ROLE -> roleRepository.existsById(subjectId);
        };
        if (!exists) {
            throw new NotFoundException("subject_not_found", "Entitlement subject not found");
        }
    }

    private IamApplication requireApplication(UUID applicationId) {
        return applicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new NotFoundException("application_not_found", "Application not found"));
    }

    private record SubjectContext(UUID userId, Set<UUID> orgIds, Set<UUID> roleIds) {}
}
