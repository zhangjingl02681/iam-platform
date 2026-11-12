package com.enterprise.iam.identity.web;

import com.enterprise.iam.common.exception.BusinessException;
import com.enterprise.iam.common.web.PageResponse;
import com.enterprise.iam.identity.service.ImportJobService;
import com.enterprise.iam.identity.web.dto.CreateImportJobRequest;
import com.enterprise.iam.identity.web.dto.ImportJobResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/import-jobs")
@PreAuthorize("hasRole('ADMIN')")
public class ImportJobController {

    private final ImportJobService importJobService;

    public ImportJobController(ImportJobService importJobService) {
        this.importJobService = importJobService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImportJobResponse> create(@Valid @RequestBody CreateImportJobRequest request) {
        ImportJobResponse created = importJobService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/import-jobs/" + created.id())).body(created);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportJobResponse> upload(
            @RequestParam(name = "type", defaultValue = ImportJobService.USER_CSV) String type,
            @RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid_csv", "CSV file is required");
        }
        String csv;
        try {
            csv = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid_csv", "Unable to read uploaded file");
        }
        String fileRef = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                ? "upload:csv"
                : file.getOriginalFilename();
        ImportJobResponse created = importJobService.createAndParse(type, fileRef, csv);
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
