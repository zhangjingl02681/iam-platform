package com.enterprise.iam.policy.service;

import com.enterprise.iam.application.repo.IamApplicationRepository;
import com.enterprise.iam.audit.AuditService;
import com.enterprise.iam.common.SecurityUtils;
import com.enterprise.iam.common.exception.BusinessException;
import com.enterprise.iam.common.exception.ConflictException;
import com.enterprise.iam.common.exception.NotFoundException;
import com.enterprise.iam.identity.repo.IamOrgRepository;
import com.enterprise.iam.policy.domain.IamPolicy;
import com.enterprise.iam.policy.domain.IamPolicyBinding;
import com.enterprise.iam.policy.domain.IamPolicyVersion;
import com.enterprise.iam.policy.domain.PolicyLifecycleStatus;
import com.enterprise.iam.policy.repo.IamPolicyBindingRepository;
import com.enterprise.iam.policy.repo.IamPolicyRepository;
import com.enterprise.iam.policy.repo.IamPolicyVersionRepository;
import com.enterprise.iam.policy.web.dto.CreatePolicyBindingRequest;
import com.enterprise.iam.policy.web.dto.CreatePolicyRequest;
import com.enterprise.iam.policy.web.dto.CreatePolicyVersionRequest;
import com.enterprise.iam.policy.web.dto.PolicyBindingResponse;
import com.enterprise.iam.policy.web.dto.PolicyResponse;
import com.enterprise.iam.policy.web.dto.PolicyVersionResponse;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PolicyService {

    private final IamPolicyRepository policyRepository;
    private final IamPolicyVersionRepository versionRepository;
    private final IamPolicyBindingRepository bindingRepository;
    private final IamApplicationRepository applicationRepository;
    private final IamOrgRepository orgRepository;
    private final PolicyDocuments documents;
    private final AuditService auditService;

    public PolicyService(
            IamPolicyRepository policyRepository,
            IamPolicyVersionRepository versionRepository,
            IamPolicyBindingRepository bindingRepository,
            IamApplicationRepository applicationRepository,
            IamOrgRepository orgRepository,
            PolicyDocuments documents,
            AuditService auditService) {
        this.policyRepository = policyRepository;
        this.versionRepository = versionRepository;
        this.bindingRepository = bindingRepository;
        this.applicationRepository = applicationRepository;
        this.orgRepository = orgRepository;
        this.documents = documents;
        this.auditService = auditService;
    }

    @Transactional
    public PolicyResponse create(CreatePolicyRequest request) {
        String code = request.code().trim().toLowerCase(Locale.ROOT);
        if (policyRepository.existsByCode(code)) {
            throw new ConflictException("policy_code_exists", "Policy code already exists");
        }
        IamPolicy policy = new IamPolicy();
        policy.setCode(code);
        policy.setName(request.name().trim());
        policy.setDescription(blankToNull(request.description()));
        policy.setStatus(PolicyLifecycleStatus.DRAFT);
        policy.setCreatedBy(SecurityUtils.currentUserId().orElse(null));
        policy = policyRepository.save(policy);
        auditService.record("policy.create", "create", "policy", policy.getId(), "success", Map.of("code", code));
        return PolicyResponse.from(policy);
    }

    @Transactional(readOnly = true)
    public PolicyResponse get(UUID id) {
        return PolicyResponse.from(requirePolicy(id));
    }

    @Transactional(readOnly = true)
    public Page<PolicyResponse> list(Pageable pageable) {
        return policyRepository.findAll(pageable).map(PolicyResponse::from);
    }

    @Transactional
    public PolicyVersionResponse addVersion(UUID policyId, CreatePolicyVersionRequest request) {
        IamPolicy policy = requirePolicy(policyId);
        documents.parse(request.document());
        String checksum = documents.checksum(request.document());
        if (StringUtils.hasText(request.checksum()) && !checksum.equalsIgnoreCase(request.checksum().trim())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "checksum_mismatch", "Document checksum does not match");
        }
        int next = versionRepository.findTopByPolicyIdOrderByVersionDesc(policyId)
                .map(item -> item.getVersion() + 1)
                .orElse(1);
        IamPolicyVersion version = new IamPolicyVersion();
        version.setPolicyId(policy.getId());
        version.setVersion(next);
        version.setLanguage(blankToNull(request.language()) == null ? "json" : request.language().trim());
        version.setDocument(request.document());
        version.setChecksum(checksum);
        version.setStatus(PolicyLifecycleStatus.DRAFT);
        version.setComment(blankToNull(request.comment()));
        version = versionRepository.save(version);
        auditService.record(
                "policy.version.add",
                "create",
                "policy",
                policyId,
                "success",
                Map.of("versionId", version.getId().toString(), "version", next));
        return PolicyVersionResponse.from(version);
    }

    @Transactional(readOnly = true)
    public List<PolicyVersionResponse> listVersions(UUID policyId) {
        requirePolicy(policyId);
        return versionRepository.findByPolicyIdOrderByVersionAsc(policyId).stream()
                .map(PolicyVersionResponse::from)
                .toList();
    }

    @Transactional
    public PolicyVersionResponse publish(UUID policyId, UUID versionId) {
        IamPolicy policy = requirePolicy(policyId);
        IamPolicyVersion version = requireVersion(policyId, versionId);
        if (version.getStatus() != PolicyLifecycleStatus.PUBLISHED) {
            version.setStatus(PolicyLifecycleStatus.PUBLISHED);
            version.setPublishedAt(Instant.now());
            version.setPublishedBy(SecurityUtils.currentUserId().orElse(null));
            version = versionRepository.save(version);
        }
        policy.setStatus(PolicyLifecycleStatus.PUBLISHED);
        policyRepository.save(policy);
        auditService.record(
                "policy.publish",
                "update",
                "policy",
                policyId,
                "success",
                Map.of("versionId", version.getId().toString(), "version", version.getVersion()));
        return PolicyVersionResponse.from(version);
    }

    @Transactional
    public PolicyBindingResponse bind(UUID policyId, CreatePolicyBindingRequest request) {
        requirePolicy(policyId);
        IamPolicyVersion version = requireVersion(policyId, request.versionId());
        if (version.getStatus() != PolicyLifecycleStatus.PUBLISHED) {
            throw new ConflictException("version_not_published", "Only a published policy version can be bound");
        }
        String targetType = PolicyDocuments.normalizeTargetType(request.targetType());
        validateTarget(targetType, request.targetId());
        if (bindingRepository.existsByVersionIdAndTargetTypeAndTargetId(
                version.getId(), targetType, request.targetId())) {
            throw new ConflictException("binding_exists", "This policy binding already exists");
        }
        IamPolicyBinding binding = new IamPolicyBinding();
        binding.setPolicyId(policyId);
        binding.setVersionId(version.getId());
        binding.setTargetType(targetType);
        binding.setTargetId(request.targetId());
        binding = bindingRepository.save(binding);
        auditService.record(
                "policy.bind",
                "create",
                "policy",
                policyId,
                "success",
                Map.of(
                        "bindingId", binding.getId().toString(),
                        "versionId", version.getId().toString(),
                        "targetType", targetType,
                        "targetId", request.targetId().toString()));
        return PolicyBindingResponse.from(binding);
    }

    @Transactional(readOnly = true)
    public List<PolicyBindingResponse> listBindings(UUID policyId) {
        requirePolicy(policyId);
        return bindingRepository.findByPolicyIdOrderByCreatedAtAsc(policyId).stream()
                .map(PolicyBindingResponse::from)
                .toList();
    }

    private void validateTarget(String targetType, UUID targetId) {
        boolean exists = switch (targetType) {
            case PolicyDocuments.TARGET_APPLICATION -> applicationRepository.existsById(targetId);
            case PolicyDocuments.TARGET_ORG -> orgRepository.existsById(targetId);
            default -> false;
        };
        if (!exists) {
            throw new NotFoundException("binding_target_not_found", "Binding target not found");
        }
    }

    private IamPolicy requirePolicy(UUID id) {
        return policyRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("policy_not_found", "Policy not found"));
    }

    private IamPolicyVersion requireVersion(UUID policyId, UUID versionId) {
        return versionRepository
                .findByPolicyIdAndId(policyId, versionId)
                .orElseThrow(() -> new NotFoundException("policy_version_not_found", "Policy version not found"));
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
