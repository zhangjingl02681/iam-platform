package com.enterprise.iam.identity.service;

import com.enterprise.iam.audit.AuditService;
import com.enterprise.iam.common.SecurityUtils;
import com.enterprise.iam.common.exception.NotFoundException;
import com.enterprise.iam.identity.domain.IamImportJob;
import com.enterprise.iam.identity.domain.ImportJobStatus;
import com.enterprise.iam.identity.repo.IamImportJobRepository;
import com.enterprise.iam.identity.web.dto.CreateImportJobRequest;
import com.enterprise.iam.identity.web.dto.ImportJobResponse;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportJobService {

    private final IamImportJobRepository repository;
    private final AuditService auditService;

    public ImportJobService(IamImportJobRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional
    public ImportJobResponse create(CreateImportJobRequest request) {
        IamImportJob job = new IamImportJob();
        job.setType(request.type().trim());
        job.setFileRef(request.fileRef());
        job.setStatus(ImportJobStatus.PENDING);
        job.setTotal(0);
        job.setSuccessCount(0);
        job.setFailedCount(0);
        job.setCreatedBy(SecurityUtils.currentUserId().orElse(null));
        job = repository.save(job);
        auditService.record("import_job.create", "create", "import_job", job.getId(), "success",
                Map.of("type", job.getType()));
        return ImportJobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public ImportJobResponse get(UUID id) {
        return ImportJobResponse.from(repository.findById(id)
                .orElseThrow(() -> new NotFoundException("import_job_not_found", "Import job not found")));
    }

    @Transactional(readOnly = true)
    public Page<ImportJobResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(ImportJobResponse::from);
    }
}
