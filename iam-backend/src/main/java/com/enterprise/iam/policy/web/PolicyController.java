package com.enterprise.iam.policy.web;

import com.enterprise.iam.common.web.PageResponse;
import com.enterprise.iam.policy.service.PolicyService;
import com.enterprise.iam.policy.web.dto.CreatePolicyBindingRequest;
import com.enterprise.iam.policy.web.dto.CreatePolicyRequest;
import com.enterprise.iam.policy.web.dto.CreatePolicyVersionRequest;
import com.enterprise.iam.policy.web.dto.PolicyBindingResponse;
import com.enterprise.iam.policy.web.dto.PolicyResponse;
import com.enterprise.iam.policy.web.dto.PolicyVersionResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/policies")
@PreAuthorize("hasRole('ADMIN')")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @PostMapping
    public ResponseEntity<PolicyResponse> create(@Valid @RequestBody CreatePolicyRequest request) {
        PolicyResponse created = policyService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/policies/" + created.id())).body(created);
    }

    @GetMapping
    public PageResponse<PolicyResponse> list(@PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.from(policyService.list(pageable));
    }

    @GetMapping("/{id}")
    public PolicyResponse get(@PathVariable UUID id) {
        return policyService.get(id);
    }

    @PostMapping("/{id}/versions")
    public ResponseEntity<PolicyVersionResponse> addVersion(
            @PathVariable UUID id, @Valid @RequestBody CreatePolicyVersionRequest request) {
        PolicyVersionResponse created = policyService.addVersion(id, request);
        return ResponseEntity.created(URI.create("/api/v1/policies/" + id + "/versions/" + created.id()))
                .body(created);
    }

    @GetMapping("/{id}/versions")
    public List<PolicyVersionResponse> versions(@PathVariable UUID id) {
        return policyService.listVersions(id);
    }

    @PostMapping("/{id}/versions/{versionId}/publish")
    public PolicyVersionResponse publish(@PathVariable UUID id, @PathVariable UUID versionId) {
        return policyService.publish(id, versionId);
    }

    @PostMapping("/{id}/bindings")
    public ResponseEntity<PolicyBindingResponse> bind(
            @PathVariable UUID id, @Valid @RequestBody CreatePolicyBindingRequest request) {
        PolicyBindingResponse created = policyService.bind(id, request);
        return ResponseEntity.created(URI.create("/api/v1/policies/" + id + "/bindings/" + created.id()))
                .body(created);
    }

    @GetMapping("/{id}/bindings")
    public List<PolicyBindingResponse> bindings(@PathVariable UUID id) {
        return policyService.listBindings(id);
    }
}
