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
import com.enterprise.iam.identity.repo.IamUserAssignmentRepository;
import com.enterprise.iam.identity.repo.IamUserAttributeRepository;
import com.enterprise.iam.identity.repo.IamUserRepository;
import com.enterprise.iam.identity.web.dto.AssignOrgRequest;
import com.enterprise.iam.identity.web.dto.AttributeValueRequest;
import com.enterprise.iam.identity.web.dto.CreateUserRequest;
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
    private final PasswordService passwordService;
    private final AuditService auditService;

    public UserService(
            IamUserRepository userRepository,
            IamOrgRepository orgRepository,
            IamPositionRepository positionRepository,
            IamUserAssignmentRepository assignmentRepository,
            IamUserAttributeRepository attributeRepository,
            PasswordService passwordService,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.orgRepository = orgRepository;
        this.positionRepository = positionRepository;
        this.assignmentRepository = assignmentRepository;
        this.attributeRepository = attributeRepository;
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

    private UserResponse toResponse(IamUser user) {
        List<IamUserAssignment> assignments = assignmentRepository.findByUserId(user.getId());
        List<IamUserAttribute> attributes = attributeRepository.findByUserIdOrderByAttrCodeAsc(user.getId());
        return UserResponse.from(user, assignments, attributes);
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
