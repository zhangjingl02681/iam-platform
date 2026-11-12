package com.enterprise.iam.oidc.web;

import com.enterprise.iam.oidc.SigningKeyService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/signing-keys")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSigningKeyController {

    private final SigningKeyService signingKeyService;

    public AdminSigningKeyController(SigningKeyService signingKeyService) {
        this.signingKeyService = signingKeyService;
    }

    @GetMapping
    public List<SigningKeyResponse> list() {
        return signingKeyService.list();
    }

    @PostMapping("/rotate")
    public SigningKeyRotationResponse rotate() {
        return signingKeyService.rotate();
    }

    @PostMapping("/{kid}/retire")
    public SigningKeyResponse retire(@PathVariable String kid) {
        return signingKeyService.retire(kid);
    }
}
