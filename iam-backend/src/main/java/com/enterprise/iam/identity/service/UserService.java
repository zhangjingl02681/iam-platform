package com.enterprise.iam.identity.service;

import com.enterprise.iam.audit.AuditService;
import com.enterprise.iam.common.SecurityUtils;
import com.enterprise.iam.common.exception.BusinessException;
import com.enterprise.iam.common.exception.ConflictException;
import com.enterprise.iam.common.exception.NotFoundException;
import com.enterprise.iam.identity.domain.IamOrg;
import com.enterprise.iam.identity.domain.IamUser;
import com.enterprise.iam.identity.domain.IamUserAssignment;
import com.enterprise.iam.identity.domain.IamUserAttribute;
import com.enterprise.iam.identity.domain.UserStatus;
import com.enterprise.iam.identity.repo.IamOrgRepository;
import com.enterprise.iam.identity.repo.IamPositionRepository;
import com.enterprise.iam.identity.repo.IamRoleRepository;
import com.enterprise.iam.identity.repo.IamUserAssignmentRepository;
import com.enterprise.iam.identity.repo.IamUserAttributeRepository;
import com.enterprise.iam.identity.repo.IamUserRepository;
import com.enterprise.iam.identity.repo.IamUserRoleRepository;
import com.enterprise.iam.identity.web.dto.AssignOrgRequest;
import com.enterprise.iam.identity.web.dto.AttributeValueRequest;
import com.enterprise.iam.identity.web.dto.CreateUserRequest;
import com.enterprise.iam.identity.web.dto.RoleResponse;
import com.enterprise.iam.identity.web.dto.UpdateUserRequest;
import com.enterprise.iam.identity.web.dto.UserResponse;
import java.util.LinkedHashMap;
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
public class UserService {

    private final IamUserRepository userRepository;
    private final IamOrgRepository orgRepository;
    private final IamPositionRepository positionRepository;
    private final IamUserAssignmentRepository assignmentRepository;
    private final IamUserAttributeRepository attributeRepository;
    private final IamUserRoleRepository userRoleRepository;
    private final IamRoleRepository roleRepository;
    private final PasswordService passwordService;
    private final AuditService auditService;

    public UserService(
            IamUserRepository userRepository,
            IamOrgRepository orgRepository,
            IamPositionRepository positionRepository,
            IamUserAssignmentRepository assignmentRepository,
            IamUserAttributeRepository attributeRepository,
            IamUserRoleRepository userRoleRepository,
            IamRoleRepository roleRepository,
            PasswordService passwordService,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.orgRepository = orgRepository;
        this.positionRepository = positionRepository;
        this.assignmentRepository = assignmentRepository;
        this.attributeRepository = attributeRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.passwordService = passwordService;
        this.auditService = auditService;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String username = normalizeUsername(request.username());
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("username_taken", "Username is already taken");
        }
        String employeeNo = blankToNull(request.employeeNo());
        if (employeeNo != null && userRepository.existsByEmployeeNo(employeeNo)) {
            throw new ConflictException("employee_no_taken", "Employee number is already taken");
        }
        IamUser user = new IamUser();
        user.setUsername(username);
        user.setEmployeeNo(employeeNo);
        user.setDisplayName(request.displayName().trim());
        user.setEmail(blankToNull(request.email()));
        user.setMobile(blankToNull(request.mobile()));
        user.setStatus(UserStatus.ACTIVE);
        UUID actor = SecurityUtils.currentUserId().orElse(null);
        user.setCreatedBy(actor);
        user.setUpdatedBy(actor);
        if (request.primaryOrgId() != null) {
            requireOrg(request.primaryOrgId());
            user.setPrimaryOrgId(request.primaryOrgId());
        }
        user = userRepository.save(user);
        if (request.primaryOrgId() != null) {
            IamUserAssignment assignment = new IamUserAssignment();
            assignment.setUserId(user.getId());
            assignment.setOrgId(request.primaryOrgId());
            assignment.setPrimary(true);
            assignmentRepository.save(assignment);
        }
        if (StringUtils.hasText(request.password())) {
            passwordService.setPassword(user.getId(), request.password(), false, false);
        }
        if (request.attributes() != null && !request.attributes().isEmpty()) {
            replaceAttributes(user.getId(), request.attributes(), false);
        }
        auditService.record("user.create", "create", "user", user.getId(), "success",
                Map.of("username", username));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return toResponse(requireUser(id));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> search(String username, UserStatus status, Pageable pageable) {
        Page<IamUser> page;
        boolean hasUsername = StringUtils.hasText(username);
        if (hasUsername && status != null) {
            page = userRepository.findByUsernameContainingIgnoreCaseAndStatus(username.trim(), status, pageable);
        } else if (hasUsername) {
            page = userRepository.findByUsernameContainingIgnoreCase(username.trim(), pageable);
        } else if (status != null) {
            page = userRepository.findByStatus(status, pageable);
        } else {
            page = userRepository.findAll(pageable);
        }
        return page.map(this::toResponse);
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        IamUser user = requireUser(id);
        if (request.status() != null) {
            applyStatus(user, request.status());
        }
        if (request.displayName() != null) {
            if (!StringUtils.hasText(request.displayName())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid_display_name", "Display name cannot be blank");
            }
            user.setDisplayName(request.displayName().trim());
        }
        if (request.email() != null) {
            user.setEmail(blankToNull(request.email()));
        }
        if (request.mobile() != null) {
            user.setMobile(blankToNull(request.mobile()));
        }
        if (request.employeeNo() != null) {
            String employeeNo = blankToNull(request.employeeNo());
            if (employeeNo != null && userRepository.existsByEmployeeNoAndIdNot(employeeNo, id)) {
                throw new ConflictException("employee_no_taken", "Employee number is already taken");
            }
            user.setEmployeeNo(employeeNo);
        }
        if (request.primaryOrgId() != null) {
            requireOrg(request.primaryOrgId());
            user.setPrimaryOrgId(request.primaryOrgId());
        }
        user.setUpdatedBy(SecurityUtils.currentUserId().orElse(null));
        user = userRepository.save(user);
        auditService.record("user.update", "update", "user", user.getId(), "success",
                Map.of("status", user.getStatus().code()));
        return toResponse(user);
    }

    @Transactional
    public UserResponse assignOrg(UUID userId, AssignOrgRequest request) {
        IamUser user = requireLivingUser(userId);
        requireOrg(request.orgId());
        if (request.positionId() != null && !positionRepository.existsById(request.positionId())) {
            throw new NotFoundException("position_not_found", "Position not found");
        }
        boolean primary = Boolean.TRUE.equals(request.primary());
        if (primary) {
            assignmentRepository.findByUserIdAndPrimaryIsTrue(userId).ifPresent(existing -> {
                existing.setPrimary(false);
                assignmentRepository.save(existing);
            });
        }
        IamUserAssignment assignment = new IamUserAssignment();
        assignment.setUserId(userId);
        assignment.setOrgId(request.orgId());
        assignment.setPositionId(request.positionId());
        assignment.setPrimary(primary);
        assignment.setValidFrom(request.validFrom());
        assignment.setValidTo(request.validTo());
        assignmentRepository.save(assignment);
        if (primary) {
            user.setPrimaryOrgId(request.orgId());
            user.setUpdatedBy(SecurityUtils.currentUserId().orElse(null));
            userRepository.save(user);
        }
        auditService.record("user.assign_org", "update", "user", userId, "success",
                Map.of("orgId", request.orgId().toString(), "primary", primary));
        return toResponse(user);
    }

    @Transactional
    public UserResponse setAttributes(UUID userId, List<AttributeValueRequest> attributes) {
        requireLivingUser(userId);
        replaceAttributes(userId, attributes, true);
        return toResponse(requireUser(userId));
    }

    private void replaceAttributes(UUID userId, List<AttributeValueRequest> attributes, boolean audit) {
        Map<String, String> unique = new LinkedHashMap<>();
        for (AttributeValueRequest item : attributes) {
            if (item == null || !StringUtils.hasText(item.attrCode())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid_attribute", "Attribute code is required");
            }
            unique.put(item.attrCode().trim(), item.valueText());
        }
        attributeRepository.deleteByUserId(userId);
        attributeRepository.flush();
        unique.forEach((code, value) -> {
            IamUserAttribute attribute = new IamUserAttribute();
            attribute.setUserId(userId);
            attribute.setAttrCode(code);
            attribute.setValueText(value);
            attributeRepository.save(attribute);
        });
        if (audit) {
            auditService.record("user.attributes.set", "update", "user", userId, "success",
                    Map.of("count", unique.size()));
        }
    }

    private void applyStatus(IamUser user, UserStatus status) {
        if (user.getStatus() == UserStatus.TERMINATED && status != UserStatus.TERMINATED) {
            throw new BusinessException(HttpStatus.CONFLICT, "user_terminated", "A terminated user cannot be reactivated");
        }
        user.setStatus(status);
    }

    private IamUser requireUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("user_not_found", "User not found"));
    }

    private IamUser requireLivingUser(UUID id) {
        IamUser user = requireUser(id);
        if (user.getStatus() == UserStatus.TERMINATED) {
            throw new BusinessException(HttpStatus.CONFLICT, "user_terminated", "User is terminated");
        }
        return user;
    }

    private IamOrg requireOrg(UUID orgId) {
        return orgRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("org_not_found", "Organization not found"));
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> rolesFor(UUID userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .map(assignment -> roleRepository.findById(assignment.getRoleId()).orElse(null))
                .filter(role -> role != null)
                .map(RoleResponse::from)
                .toList();
    }

    /**
     * Create or update a user from a bulk import row. Throws {@link BusinessException}
     * (or a subclass) when the row cannot be applied; the caller records it as a row failure.
     */
    @Transactional
    public IamUser importRow(
            String username,
            String displayName,
            String email,
            String orgCode,
            UserStatus status,
            String temporaryPassword) {
        String normalized = normalizeUsername(username);
        if (!normalized.matches("^[a-z0-9._-]{3,64}$")) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "invalid_username", "Username is invalid");
        }
        if (!StringUtils.hasText(displayName)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "invalid_display_name", "Display name is required");
        }
        IamOrg org = null;
        if (StringUtils.hasText(orgCode)) {
            org = orgRepository
                    .findByCode(orgCode.trim())
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.BAD_REQUEST,
                            "org_not_found",
                            "Organization not found: " + orgCode.trim()));
        }
        IamUser user = userRepository.findByUsername(normalized).orElse(null);
        boolean created = user == null;
        if (created) {
            user = new IamUser();
            user.setUsername(normalized);
            user.setStatus(status == null ? UserStatus.ACTIVE : status);
        } else if (user.getStatus() == UserStatus.TERMINATED && status != UserStatus.TERMINATED) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "user_terminated", "A terminated user cannot be reactivated");
        }
        user.setDisplayName(displayName.trim());
        if (email != null) {
            user.setEmail(blankToNull(email));
        }
        if (status != null) {
            applyStatus(user, status);
        }
        if (org != null) {
            user.setPrimaryOrgId(org.getId());
        }
        user = userRepository.save(user);
        if (org != null) {
            assignPrimaryOrgIfNeeded(user, org.getId());
        }
        if (StringUtils.hasText(temporaryPassword)) {
            passwordService.setPassword(user.getId(), temporaryPassword, true, false);
        }
        if (created) {
            auditService.record("user.import.create", "create", "user", user.getId(), "success",
                    Map.of("username", normalized));
        } else {
            auditService.record("user.import.update", "update", "user", user.getId(), "success",
                    Map.of("username", normalized));
        }
        return user;
    }

    private void assignPrimaryOrgIfNeeded(IamUser user, UUID orgId) {
        assignmentRepository.findByUserIdAndPrimaryIsTrue(user.getId()).ifPresent(existing -> {
            if (!orgId.equals(existing.getOrgId())) {
                existing.setPrimary(false);
                assignmentRepository.save(existing);
            }
        });
        boolean already = assignmentRepository.findByUserId(user.getId()).stream()
                .anyMatch(item -> orgId.equals(item.getOrgId()));
        if (!already) {
            IamUserAssignment assignment = new IamUserAssignment();
            assignment.setUserId(user.getId());
            assignment.setOrgId(orgId);
            assignment.setPrimary(true);
            assignmentRepository.save(assignment);
        } else {
            assignmentRepository.findByUserId(user.getId()).stream()
                    .filter(item -> orgId.equals(item.getOrgId()))
                    .forEach(item -> {
                        if (!item.isPrimary()) {
                            item.setPrimary(true);
                            assignmentRepository.save(item);
                        }
                    });
        }
        user.setPrimaryOrgId(orgId);
        userRepository.save(user);
    }

    private UserResponse toResponse(IamUser user) {
        List<IamUserAssignment> assignments = assignmentRepository.findByUserId(user.getId());
        List<IamUserAttribute> attributes = attributeRepository.findByUserIdOrderByAttrCodeAsc(user.getId());
        return UserResponse.from(user, assignments, attributes, rolesFor(user.getId()));
    }

    public static String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid_username", "Username is required");
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
