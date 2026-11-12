package com.enterprise.iam.policy.service;

import com.enterprise.iam.application.domain.IamApplication;
import com.enterprise.iam.application.repo.IamApplicationRepository;
import com.enterprise.iam.identity.domain.IamRole;
import com.enterprise.iam.identity.domain.IamUserAssignment;
import com.enterprise.iam.identity.domain.IamUserAttribute;
import com.enterprise.iam.identity.domain.RoleStatus;
import com.enterprise.iam.identity.repo.IamOrgRepository;
import com.enterprise.iam.identity.repo.IamRoleRepository;
import com.enterprise.iam.identity.repo.IamUserAssignmentRepository;
import com.enterprise.iam.identity.repo.IamUserAttributeRepository;
import com.enterprise.iam.identity.repo.IamUserRoleRepository;
import com.enterprise.iam.policy.document.PolicyDocument;
import com.enterprise.iam.policy.document.PolicyResource;
import com.enterprise.iam.policy.document.PolicyWhen;
import com.enterprise.iam.policy.domain.IamPolicyBinding;
import com.enterprise.iam.policy.domain.IamPolicyVersion;
import com.enterprise.iam.policy.domain.PolicyLifecycleStatus;
import com.enterprise.iam.policy.repo.IamPolicyBindingRepository;
import com.enterprise.iam.policy.repo.IamPolicyVersionRepository;
import com.enterprise.iam.policy.web.dto.PdpDecideResponse;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PdpService {

    private final IamPolicyBindingRepository bindingRepository;
    private final IamPolicyVersionRepository versionRepository;
    private final PolicyDocuments documents;
    private final IamApplicationRepository applicationRepository;
    private final IamOrgRepository orgRepository;
    private final IamUserAssignmentRepository assignmentRepository;
    private final IamUserAttributeRepository attributeRepository;
    private final IamUserRoleRepository userRoleRepository;
    private final IamRoleRepository roleRepository;

    public PdpService(
            IamPolicyBindingRepository bindingRepository,
            IamPolicyVersionRepository versionRepository,
            PolicyDocuments documents,
            IamApplicationRepository applicationRepository,
            IamOrgRepository orgRepository,
            IamUserAssignmentRepository assignmentRepository,
            IamUserAttributeRepository attributeRepository,
            IamUserRoleRepository userRoleRepository,
            IamRoleRepository roleRepository) {
        this.bindingRepository = bindingRepository;
        this.versionRepository = versionRepository;
        this.documents = documents;
        this.applicationRepository = applicationRepository;
        this.orgRepository = orgRepository;
        this.assignmentRepository = assignmentRepository;
        this.attributeRepository = attributeRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public PdpDecideResponse decide(
            UUID userId, String action, String resourceType, UUID resourceId, String resourceCode) {
        ResourceContext resource = resolveResource(resourceType, resourceId, resourceCode);
        SubjectContext subject = subjectContext(userId);
        List<ApplicablePolicy> applicable = findApplicable(action, resource, subject, false);
        ApplicablePolicy denied = applicable.stream().filter(item -> item.document().deny()).findFirst().orElse(null);
        if (denied != null) {
            return PdpDecideResponse.deny(
                    "deny_overrides:" + denied.policyId() + ":v" + denied.version().getVersion(),
                    denied.policyId(),
                    denied.version().getVersion());
        }
        ApplicablePolicy allowed = applicable.stream().filter(item -> item.document().allow()).findFirst().orElse(null);
        if (allowed != null) {
            return PdpDecideResponse.allow(
                    "policy:" + allowed.policyId() + ":v" + allowed.version().getVersion(),
                    allowed.policyId(),
                    allowed.version().getVersion());
        }
        return PdpDecideResponse.defaultDeny();
    }

    @Transactional(readOnly = true)
    public boolean permitsLaunch(UUID userId, IamApplication application) {
        ResourceContext resource =
                new ResourceContext(PolicyDocuments.TARGET_APPLICATION, application.getId(), application.getCode(), application.getOwnerOrgId());
        SubjectContext subject = subjectContext(userId);
        boolean applies = !findApplicable(PolicyDocuments.ACTION_APP_LAUNCH, resource, subject, true).isEmpty();
        if (!applies) {
            return true;
        }
        return decide(
                        userId,
                        PolicyDocuments.ACTION_APP_LAUNCH,
                        PolicyDocuments.TARGET_APPLICATION,
                        application.getId(),
                        application.getCode())
                .allowed();
    }

    private List<ApplicablePolicy> findApplicable(
            String action, ResourceContext resource, SubjectContext subject, boolean ignoreWhen) {
        List<IamPolicyBinding> bindings = bindingRepository.findAll();
        Map<UUID, IamPolicyVersion> versions = new HashMap<>();
        List<ApplicablePolicy> matches = new java.util.ArrayList<>();
        for (IamPolicyBinding binding : bindings) {
            IamPolicyVersion version = versions.computeIfAbsent(
                    binding.getVersionId(), id -> versionRepository.findById(id).orElse(null));
            if (version == null || version.getStatus() != PolicyLifecycleStatus.PUBLISHED) {
                continue;
            }
            PolicyDocument document;
            try {
                document = documents.parse(version.getDocument());
            } catch (RuntimeException ex) {
                continue;
            }
            if (!actionMatches(document, action) || !resourceMatches(document, resource)) {
                continue;
            }
            if (!bindingApplies(binding, resource, subject)) {
                continue;
            }
            if (!ignoreWhen && !whenMatches(document.when(), subject)) {
                continue;
            }
            matches.add(new ApplicablePolicy(version.getPolicyId(), version, document));
        }
        return matches;
    }

    private boolean bindingApplies(IamPolicyBinding binding, ResourceContext resource, SubjectContext subject) {
        String type = binding.getTargetType() == null ? "" : binding.getTargetType().toLowerCase(Locale.ROOT);
        if (PolicyDocuments.TARGET_APPLICATION.equals(type)) {
            return resource.id() != null && resource.id().equals(binding.getTargetId());
        }
        if (PolicyDocuments.TARGET_ORG.equals(type)) {
            return subject.orgIds().contains(binding.getTargetId())
                    || (resource.ownerOrgId() != null && resource.ownerOrgId().equals(binding.getTargetId()));
        }
        return false;
    }

    private boolean actionMatches(PolicyDocument document, String action) {
        if (!StringUtils.hasText(action)) {
            return false;
        }
        return document.actions().stream()
                .filter(Objects::nonNull)
                .anyMatch(item -> "*".equals(item) || item.equalsIgnoreCase(action.trim()));
    }

    private boolean resourceMatches(PolicyDocument document, ResourceContext resource) {
        return document.resources().stream().anyMatch(item -> matchesResource(item, resource));
    }

    private boolean matchesResource(PolicyResource selector, ResourceContext resource) {
        if (!wildcardOrEqual(selector.type(), resource.type())) {
            return false;
        }
        boolean idConstrained = StringUtils.hasText(selector.id()) && !"*".equals(selector.id());
        boolean codeConstrained = StringUtils.hasText(selector.code()) && !"*".equals(selector.code());
        if (idConstrained && (resource.id() == null || !selector.id().equalsIgnoreCase(resource.id().toString()))) {
            return false;
        }
        if (codeConstrained && (resource.code() == null || !selector.code().equalsIgnoreCase(resource.code()))) {
            return false;
        }
        return true;
    }

    private boolean whenMatches(PolicyWhen when, SubjectContext subject) {
        if (when == null) {
            return true;
        }
        if (when.roles() != null && !when.roles().isEmpty()) {
            Set<String> wanted = when.roles().stream()
                    .filter(StringUtils::hasText)
                    .map(value -> value.trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            if (wanted.stream().noneMatch(subject.roleCodes()::contains)) {
                return false;
            }
        }
        if (when.orgCodes() != null && !when.orgCodes().isEmpty()) {
            Set<String> wanted = when.orgCodes().stream()
                    .filter(StringUtils::hasText)
                    .map(value -> value.trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            if (wanted.stream().noneMatch(subject.orgCodes()::contains)) {
                return false;
            }
        }
        if (when.attributes() != null && !when.attributes().isEmpty()) {
            for (Map.Entry<String, String> entry : when.attributes().entrySet()) {
                if (!StringUtils.hasText(entry.getKey())) {
                    continue;
                }
                String actual = subject.attributes().get(entry.getKey().trim());
                String expected = entry.getValue() == null ? "" : entry.getValue();
                if (actual == null || !actual.equals(expected)) {
                    return false;
                }
            }
        }
        return true;
    }

    private ResourceContext resolveResource(String resourceType, UUID resourceId, String resourceCode) {
        String type = resourceType == null ? "" : resourceType.trim().toLowerCase(Locale.ROOT);
        UUID id = resourceId;
        String code = StringUtils.hasText(resourceCode) ? resourceCode.trim() : null;
        UUID ownerOrgId = null;
        if (PolicyDocuments.TARGET_APPLICATION.equals(type)) {
            IamApplication application = null;
            if (id != null) {
                application = applicationRepository.findById(id).orElse(null);
            } else if (code != null) {
                application = applicationRepository.findByCode(code.toLowerCase(Locale.ROOT)).orElse(null);
            }
            if (application != null) {
                id = application.getId();
                code = application.getCode();
                ownerOrgId = application.getOwnerOrgId();
            }
        }
        return new ResourceContext(type, id, code, ownerOrgId);
    }

    private SubjectContext subjectContext(UUID userId) {
        List<IamUserAssignment> assignments = assignmentRepository.findByUserId(userId);
        Set<UUID> orgIds = assignments.stream().map(IamUserAssignment::getOrgId).collect(Collectors.toSet());
        Set<String> orgCodes = orgIds.stream()
                .map(id -> orgRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .map(org -> org.getCode().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        Set<UUID> roleIds = new HashSet<>();
        Set<String> roleCodes = new HashSet<>();
        userRoleRepository.findByUserId(userId).forEach(assignment -> {
            IamRole role = roleRepository.findById(assignment.getRoleId()).orElse(null);
            if (role != null && role.getStatus() == RoleStatus.ACTIVE) {
                roleIds.add(role.getId());
                roleCodes.add(role.getCode().toLowerCase(Locale.ROOT));
            }
        });
        Map<String, String> attributes = new HashMap<>();
        for (IamUserAttribute attribute : attributeRepository.findByUserIdOrderByAttrCodeAsc(userId)) {
            attributes.put(attribute.getAttrCode(), attribute.getValueText());
        }
        return new SubjectContext(userId, orgIds, orgCodes, roleIds, roleCodes, attributes);
    }

    private static boolean wildcardOrEqual(String selector, String actual) {
        return !StringUtils.hasText(selector) || "*".equals(selector) || selector.equalsIgnoreCase(actual);
    }

    private record ResourceContext(String type, UUID id, String code, UUID ownerOrgId) {}

    private record SubjectContext(
            UUID userId,
            Set<UUID> orgIds,
            Set<String> orgCodes,
            Set<UUID> roleIds,
            Set<String> roleCodes,
            Map<String, String> attributes) {}

    private record ApplicablePolicy(UUID policyId, IamPolicyVersion version, PolicyDocument document) {}
}
