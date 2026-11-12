package com.enterprise.iam.identity.web;

import com.enterprise.iam.common.web.PageResponse;
import com.enterprise.iam.identity.service.ImportJobService;
import com.enterprise.iam.identity.web.dto.CreateImportJobRequest;
import com.enterprise.iam.identity.web.dto.ImportJobResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/api/v1/import-jobs")
@PreAuthorize("hasRole('ADMIN')")
public class ImportJobController {

    private final ImportJobService importJobService;

    public ImportJobController(ImportJobService importJobService) {
        this.importJobService = importJobService;
    }

    @PostMapping
    public ResponseEntity<ImportJobResponse> create(@Valid @RequestBody CreateImportJobRequest request) {
        ImportJobResponse created = importJobService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/import-jobs/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public ImportJobResponse get(@PathVariable UUID id) {
        return importJobService.get(id);
    }

    @GetMapping
    public PageResponse<ImportJobResponse> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(importJobService.list(pageable));
    }
}
