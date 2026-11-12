package com.enterprise.iam.identity.web;

import com.enterprise.iam.auth.IamPrincipal;
import com.enterprise.iam.identity.service.MfaService;
import com.enterprise.iam.identity.web.dto.MfaFactorResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}/mfa/factors")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMfaController {

    private final MfaService mfaService;

    public AdminMfaController(MfaService mfaService) {
        this.mfaService = mfaService;
    }

    @GetMapping
    public List<MfaFactorResponse> list(@PathVariable UUID userId) {
        return mfaService.listFactors(userId);
    }

    @PostMapping("/{factorId}/disable")
    public ResponseEntity<Void> disable(
            @PathVariable UUID userId,
            @PathVariable UUID factorId,
            @AuthenticationPrincipal IamPrincipal principal) {
        mfaService.disableFactor(principal.getUserId(), userId, factorId, true);
        return ResponseEntity.noContent().build();
    }
}
