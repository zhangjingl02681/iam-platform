package com.enterprise.iam.identity.web;

import com.enterprise.iam.common.web.PageResponse;
import com.enterprise.iam.identity.domain.UserStatus;
import com.enterprise.iam.identity.service.PasswordService;
import com.enterprise.iam.identity.service.RoleService;
import com.enterprise.iam.identity.service.UserService;
import com.enterprise.iam.identity.web.dto.AdminSetPasswordRequest;
import com.enterprise.iam.identity.web.dto.AssignOrgRequest;
import com.enterprise.iam.identity.web.dto.AssignRoleRequest;
import com.enterprise.iam.identity.web.dto.CreateUserRequest;
import com.enterprise.iam.identity.web.dto.RoleResponse;
import com.enterprise.iam.identity.web.dto.SetAttributesRequest;
import com.enterprise.iam.identity.web.dto.UpdateUserRequest;
import com.enterprise.iam.identity.web.dto.UserResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final PasswordService passwordService;
    private final RoleService roleService;

    public UserController(UserService userService, PasswordService passwordService, RoleService roleService) {
        this.userService = userService;
        this.passwordService = passwordService;
        this.roleService = roleService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse created = userService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public UserResponse get(@PathVariable UUID id) {
        return userService.get(id);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<UserResponse> search(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(userService.search(username, status, pageable));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @PostMapping("/{id}/assignments")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse assignOrg(@PathVariable UUID id, @Valid @RequestBody AssignOrgRequest request) {
        return userService.assignOrg(id, request);
    }

    @PutMapping("/{id}/attributes")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse setAttributes(@PathVariable UUID id, @Valid @RequestBody SetAttributesRequest request) {
        return userService.setAttributes(id, request.attributes());
    }

    @PostMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> setPassword(
            @PathVariable UUID id, @Valid @RequestBody AdminSetPasswordRequest request) {
        boolean mustChange = request.mustChange() == null || request.mustChange();
        passwordService.setPassword(id, request.newPassword(), mustChange, true);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public List<RoleResponse> listRoles(@PathVariable UUID id) {
        return roleService.listForUser(id);
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse assignRole(@PathVariable UUID id, @Valid @RequestBody AssignRoleRequest request) {
        return roleService.assignToUser(id, request.roleId());
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeRole(@PathVariable UUID id, @PathVariable UUID roleId) {
        roleService.removeFromUser(id, roleId);
        return ResponseEntity.noContent().build();
    }
}
