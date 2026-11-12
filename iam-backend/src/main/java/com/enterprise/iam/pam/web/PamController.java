package com.enterprise.iam.pam.web;

import com.enterprise.iam.auth.IamPrincipal;
import com.enterprise.iam.pam.service.PamService;
import com.enterprise.iam.pam.web.dto.CreatePamAccountRequest;
import com.enterprise.iam.pam.web.dto.CreatePamRequestRequest;
import com.enterprise.iam.pam.web.dto.CreatePamTargetRequest;
import com.enterprise.iam.pam.web.dto.DenyPamRequest;
import com.enterprise.iam.pam.web.dto.PamAccountResponse;
import com.enterprise.iam.pam.web.dto.PamCheckoutResponse;
import com.enterprise.iam.pam.web.dto.PamRequestResponse;
import com.enterprise.iam.pam.web.dto.PamSecretResponse;
import com.enterprise.iam.pam.web.dto.PamTargetResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pam")
@PreAuthorize("isAuthenticated()")
public class PamController {

    private final PamService pamService;

    public PamController(PamService pamService) {
        this.pamService = pamService;
    }

    @PostMapping("/targets")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PamTargetResponse> createTarget(@Valid @RequestBody CreatePamTargetRequest request) {
        PamTargetResponse created = pamService.createTarget(request);
        return ResponseEntity.created(URI.create("/api/v1/pam/targets/" + created.id())).body(created);
    }

    @GetMapping("/targets")
    public List<PamTargetResponse> listTargets() {
        return pamService.listTargets();
    }

    @PostMapping("/targets/{targetId}/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PamAccountResponse> createAccount(
            @PathVariable UUID targetId, @Valid @RequestBody CreatePamAccountRequest request) {
        PamAccountResponse created = pamService.createAccount(targetId, request);
        return ResponseEntity.created(URI.create("/api/v1/pam/accounts/" + created.id())).body(created);
    }

    @GetMapping("/targets/{targetId}/accounts")
    public List<PamAccountResponse> listAccounts(@PathVariable UUID targetId) {
        return pamService.listAccounts(targetId);
    }

    @PostMapping("/requests")
    public ResponseEntity<PamRequestResponse> createRequest(
            @AuthenticationPrincipal IamPrincipal principal, @Valid @RequestBody CreatePamRequestRequest request) {
        PamRequestResponse created = pamService.createRequest(principal.getUserId(), request);
        return ResponseEntity.created(URI.create("/api/v1/pam/requests/" + created.id())).body(created);
    }

    @GetMapping("/requests")
    public List<PamRequestResponse> listRequests(@AuthenticationPrincipal IamPrincipal principal) {
        return pamService.listRequests(principal.getUserId(), isAdmin(principal));
    }

    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public PamRequestResponse approve(@PathVariable UUID id, @AuthenticationPrincipal IamPrincipal principal) {
        return pamService.approve(id, principal.getUserId());
    }

    @PostMapping("/requests/{id}/deny")
    @PreAuthorize("hasRole('ADMIN')")
    public PamRequestResponse deny(
            @PathVariable UUID id,
            @AuthenticationPrincipal IamPrincipal principal,
            @RequestBody(required = false) DenyPamRequest request) {
        return pamService.deny(id, principal.getUserId(), request == null ? null : request.reason());
    }

    @PostMapping("/requests/{id}/checkout")
    public PamSecretResponse checkout(@PathVariable UUID id, @AuthenticationPrincipal IamPrincipal principal) {
        return pamService.checkout(id, principal.getUserId());
    }

    @GetMapping("/checkouts")
    public List<PamCheckoutResponse> listCheckouts(@AuthenticationPrincipal IamPrincipal principal) {
        return pamService.listCheckouts(principal.getUserId(), isAdmin(principal));
    }

    @PostMapping("/checkouts/{id}/check-in")
    public ResponseEntity<Void> checkIn(@PathVariable UUID id, @AuthenticationPrincipal IamPrincipal principal) {
        pamService.checkIn(id, principal.getUserId(), isAdmin(principal));
        return ResponseEntity.noContent().build();
    }

    private static boolean isAdmin(IamPrincipal principal) {
        return principal.getAuthorities().stream().anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
    }
}
