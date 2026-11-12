package com.enterprise.iam.application.web;

import com.enterprise.iam.application.domain.ApplicationStatus;
import com.enterprise.iam.application.service.ApplicationService;
import com.enterprise.iam.application.web.dto.ApplicationResponse;
import com.enterprise.iam.application.web.dto.CreateApplicationRequest;
import com.enterprise.iam.application.web.dto.UpdateApplicationRequest;
import com.enterprise.iam.common.web.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApplicationResponse> create(@Valid @RequestBody CreateApplicationRequest request) {
        ApplicationResponse created = applicationService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/applications/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApplicationResponse get(@PathVariable UUID id) {
        return applicationService.get(id);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<ApplicationResponse> search(
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false) ApplicationStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(applicationService.search(query, status, pageable));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApplicationResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateApplicationRequest request) {
        return applicationService.update(id, request);
    }
}
