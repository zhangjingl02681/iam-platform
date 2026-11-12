package com.enterprise.iam.identity.web;

import com.enterprise.iam.identity.service.OrgService;
import com.enterprise.iam.identity.web.dto.CreateOrgRequest;
import com.enterprise.iam.identity.web.dto.OrgResponse;
import com.enterprise.iam.identity.web.dto.OrgTreeNode;
import com.enterprise.iam.identity.web.dto.UpdateOrgRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orgs")
public class OrgController {

    private final OrgService orgService;

    public OrgController(OrgService orgService) {
        this.orgService = orgService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrgResponse> create(@Valid @RequestBody CreateOrgRequest request) {
        OrgResponse created = orgService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/orgs/" + created.id())).body(created);
    }

    @GetMapping("/tree")
    @PreAuthorize("isAuthenticated()")
    public List<OrgTreeNode> tree() {
        return orgService.tree();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public OrgResponse get(@PathVariable UUID id) {
        return orgService.get(id);
    }

    @GetMapping("/{id}/children")
    @PreAuthorize("isAuthenticated()")
    public List<OrgResponse> children(@PathVariable UUID id) {
        return orgService.listChildren(id);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<OrgResponse> list(@RequestParam(required = false) UUID parentId) {
        return orgService.listChildren(parentId);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public OrgResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateOrgRequest request) {
        return orgService.update(id, request);
    }
}
