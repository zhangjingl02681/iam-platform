package com.enterprise.iam.identity.web;

import com.enterprise.iam.auth.IamPrincipal;
import com.enterprise.iam.identity.service.MfaService;
import com.enterprise.iam.identity.web.dto.MfaConfirmRequest;
import com.enterprise.iam.identity.web.dto.MfaConfirmResponse;
import com.enterprise.iam.identity.web.dto.MfaEnrollRequest;
import com.enterprise.iam.identity.web.dto.MfaEnrollmentResponse;
import com.enterprise.iam.identity.web.dto.MfaFactorResponse;
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
@RequestMapping("/api/v1/me/mfa")
@PreAuthorize("isAuthenticated()")
public class MeMfaController {

    private final MfaService mfaService;

    public MeMfaController(MfaService mfaService) {
        this.mfaService = mfaService;
    }

    @PostMapping("/totp/enrollments")
    public ResponseEntity<MfaEnrollmentResponse> enroll(
            @AuthenticationPrincipal IamPrincipal principal, @RequestBody(required = false) MfaEnrollRequest request) {
        String label = request == null ? null : request.label();
        MfaEnrollmentResponse created = mfaService.startTotpEnrollment(principal.getUserId(), label);
        return ResponseEntity.created(URI.create("/api/v1/me/mfa/factors/" + created.id())).body(created);
    }

    @PostMapping("/totp/enrollments/{id}/confirm")
    public MfaConfirmResponse confirm(
            @AuthenticationPrincipal IamPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody MfaConfirmRequest request) {
        return mfaService.confirmEnrollment(principal.getUserId(), id, request.code());
    }

    @GetMapping("/factors")
    public List<MfaFactorResponse> list(@AuthenticationPrincipal IamPrincipal principal) {
        return mfaService.listFactors(principal.getUserId());
    }

    @PostMapping("/factors/{id}/disable")
    public ResponseEntity<Void> disable(@AuthenticationPrincipal IamPrincipal principal, @PathVariable UUID id) {
        mfaService.disableFactor(principal.getUserId(), principal.getUserId(), id, false);
        return ResponseEntity.noContent().build();
    }
}
