package com.enterprise.iam.identity.web;

import com.enterprise.iam.application.service.ApplicationService;
import com.enterprise.iam.application.web.dto.ApplicationResponse;
import com.enterprise.iam.auth.IamPrincipal;
import com.enterprise.iam.identity.domain.CredentialType;
import com.enterprise.iam.identity.repo.IamCredentialRepository;
import com.enterprise.iam.identity.service.PasswordService;
import com.enterprise.iam.identity.service.UserService;
import com.enterprise.iam.identity.web.dto.ChangePasswordRequest;
import com.enterprise.iam.identity.web.dto.MeResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@PreAuthorize("isAuthenticated()")
public class MeController {

    private final UserService userService;
    private final PasswordService passwordService;
    private final IamCredentialRepository credentialRepository;
    private final ApplicationService applicationService;

    public MeController(
            UserService userService,
            PasswordService passwordService,
            IamCredentialRepository credentialRepository,
            ApplicationService applicationService) {
        this.userService = userService;
        this.passwordService = passwordService;
        this.credentialRepository = credentialRepository;
        this.applicationService = applicationService;
    }

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal IamPrincipal principal) {
        boolean mustChange = credentialRepository
                .findByUserIdAndType(principal.getUserId(), CredentialType.PASSWORD)
                .map(credential -> credential.isMustChange())
                .orElse(false);
        List<String> authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        boolean admin = authorities.contains("ROLE_ADMIN");
        return new MeResponse(userService.get(principal.getUserId()), mustChange, admin, authorities);
    }

    @GetMapping("/applications")
    public List<ApplicationResponse> applications() {
        return applicationService.listLaunchableForCurrentUser();
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal IamPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        passwordService.changeOwnPassword(principal.getUserId(), request.oldPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
