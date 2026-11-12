package com.enterprise.iam.identity.service;

import com.enterprise.iam.audit.AuditService;
import com.enterprise.iam.common.exception.BusinessException;
import com.enterprise.iam.common.exception.ConflictException;
import com.enterprise.iam.common.exception.NotFoundException;
import com.enterprise.iam.identity.domain.IamRole;
import com.enterprise.iam.identity.domain.IamUser;
import com.enterprise.iam.identity.domain.IamUserRole;
import com.enterprise.iam.identity.domain.RoleStatus;
import com.enterprise.iam.identity.domain.UserStatus;
import com.enterprise.iam.identity.repo.IamRoleRepository;
import com.enterprise.iam.identity.repo.IamUserRepository;
import com.enterprise.iam.identity.repo.IamUserRoleRepository;
import com.enterprise.iam.identity.web.dto.CreateRoleRequest;
import com.enterprise.iam.identity.web.dto.RoleResponse;
import com.enterprise.iam.identity.web.dto.UpdateRoleRequest;
import com.enterprise.iam.identity.web.dto.UserResponse;
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
public class RoleService {

    public static final String ADMIN_ROLE_CODE = "admin";

    private final IamRoleRepository roleRepository;
    private final IamUserRoleRepository userRoleRepository;
    private final IamUserRepository userRepository;
    private final UserService userService;
    private final AuditService auditService;

    public RoleService(
            IamRoleRepository roleRepository,
            IamUserRoleRepository userRoleRepository,
            IamUserRepository userRepository,
            UserService userService,
            AuditService auditService) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.auditService = auditService;
    }

    @Transactional
    public RoleResponse create(CreateRoleRequest request) {
        String code = normalizeCode(request.code());
        if (roleRepository.existsByCode(code)) {
            throw new ConflictException("role_code_exists", "Role code already exists");
        }
        IamRole role = new IamRole();
        role.setCode(code);
        role.setName(request.name().trim());
        role.setDescription(blankToNull(request.description()));
        role.setStatus(request.status() == null ? RoleStatus.ACTIVE : request.status());
        role = roleRepository.save(role);
        auditService.record("role.create", "create", "role", role.getId(), "success", Map.of("code", code));
        return RoleResponse.from(role);
    }

    @Transactional(readOnly = true)
    public RoleResponse get(UUID id) {
        return RoleResponse.from(requireRole(id));
    }

    @Transactional(readOnly = true)
    public Page<RoleResponse> list(Pageable pageable) {
        return roleRepository.findAll(pageable).map(RoleResponse::from);
    }

    @Transactional
    public RoleResponse update(UUID id, UpdateRoleRequest request) {
        IamRole role = requireRole(id);
        if (request.name() != null) {
            if (!StringUtils.hasText(request.name())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid_role_name", "Role name cannot be blank");
            }
            role.setName(request.name().trim());
        }
        if (request.description() != null) {
            role.setDescription(blankToNull(request.description()));
        }
        if (request.status() != null) {
            applyStatus(role, request.status());
        }
        role = roleRepository.save(role);
        auditService.record(
                "role.update",
                "update",
                "role",
                role.getId(),
                "success",
                Map.of("status", role.getStatus().code()));
        return RoleResponse.from(role);
    }

    @Transactional
    public void delete(UUID id) {
        IamRole role = requireRole(id);
        if (isAdminRole(role)) {
            throw new ConflictException("admin_role_protected", "The built-in admin role cannot be deleted");
        }
        if (userRoleRepository.countByRoleId(id) > 0) {
            throw new ConflictException("role_in_use", "Role is still assigned to users");
        }
        roleRepository.delete(role);
        auditService.record("role.delete", "delete", "role", id, "success", Map.of("code", role.getCode()));
    }

    @Transactional
    public UserResponse assignToUser(UUID userId, UUID roleId) {
        IamUser user = requireLivingUser(userId);
        IamRole role = requireRole(roleId);
        if (role.getStatus() != RoleStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.CONFLICT, "role_disabled", "Cannot assign a disabled role");
        }
        if (!userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            IamUserRole assignment = new IamUserRole();
            assignment.setUserId(userId);
            assignment.setRoleId(roleId);
            userRoleRepository.save(assignment);
            auditService.record(
                    "user.assign_role",
                    "update",
                    "user",
                    userId,
                    "success",
                    Map.of("roleId", roleId.toString(), "roleCode", role.getCode()));
        }
        return userService.get(user.getId());
    }

    @Transactional
    public void removeFromUser(UUID userId, UUID roleId) {
        requireUser(userId);
        IamRole role = requireRole(roleId);
        IamUserRole assignment = userRoleRepository
                .findByUserIdAndRoleId(userId, roleId)
                .orElseThrow(() -> new NotFoundException("user_role_not_found", "User does not have this role"));
        if (isAdminRole(role) && countActiveAdminsExcluding(userId) == 0) {
            throw new ConflictException("last_admin", "Cannot remove the last remaining administrator");
        }
        userRoleRepository.delete(assignment);
        auditService.record(
                "user.remove_role",
                "update",
                "user",
                userId,
                "success",
                Map.of("roleId", roleId.toString(), "roleCode", role.getCode()));
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> listForUser(UUID userId) {
        requireUser(userId);
        return userService.rolesFor(userId);
    }

    private void applyStatus(IamRole role, RoleStatus status) {
        if (status == RoleStatus.DISABLED && isAdminRole(role)) {
            throw new ConflictException(
                    "admin_role_protected", "Disabling the built-in admin role would lock out administrators");
        }
        role.setStatus(status);
    }

    private long countActiveAdminsExcluding(UUID userId) {
        IamRole admin = roleRepository.findByCode(ADMIN_ROLE_CODE).orElse(null);
        if (admin == null || admin.getStatus() != RoleStatus.ACTIVE) {
            return 0;
        }
        return userRoleRepository.findByRoleId(admin.getId()).stream()
                .filter(assignment -> !assignment.getUserId().equals(userId))
                .map(assignment -> userRepository.findById(assignment.getUserId()).orElse(null))
                .filter(user -> user != null && user.getStatus() == UserStatus.ACTIVE)
                .count();
    }

    private IamRole requireRole(UUID id) {
        return roleRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("role_not_found", "Role not found"));
    }

    private IamUser requireUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("user_not_found", "User not found"));
    }

    private IamUser requireLivingUser(UUID id) {
        IamUser user = requireUser(id);
        if (user.getStatus() == UserStatus.TERMINATED) {
            throw new BusinessException(HttpStatus.CONFLICT, "user_terminated", "User is terminated");
        }
        return user;
    }

    public static boolean isAdminRole(IamRole role) {
        return role != null && ADMIN_ROLE_CODE.equalsIgnoreCase(role.getCode());
    }

    public static String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid_role_code", "Role code is required");
        }
        return code.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
