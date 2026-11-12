package com.enterprise.iam.policy.web;

import com.enterprise.iam.auth.IamPrincipal;
import com.enterprise.iam.policy.service.PdpService;
import com.enterprise.iam.policy.web.dto.PdpDecideRequest;
import com.enterprise.iam.policy.web.dto.PdpDecideResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pdp")
@PreAuthorize("isAuthenticated()")
public class PdpController {

    private final PdpService pdpService;

    public PdpController(PdpService pdpService) {
        this.pdpService = pdpService;
    }

    @PostMapping("/decide")
    public PdpDecideResponse decide(
            @AuthenticationPrincipal IamPrincipal principal, @Valid @RequestBody PdpDecideRequest request) {
        return pdpService.decide(
                principal.getUserId(),
                request.action(),
                request.resourceType(),
                request.resourceId(),
                request.resourceCode());
    }
}
