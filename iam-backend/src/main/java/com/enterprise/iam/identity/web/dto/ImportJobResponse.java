package com.enterprise.iam.identity.web.dto;

import com.enterprise.iam.identity.domain.IamImportJob;
import com.enterprise.iam.identity.domain.ImportJobStatus;
import java.time.Instant;
import java.util.UUID;

public record ImportJobResponse(
        UUID id,
        String type,
        String fileRef,
        ImportJobStatus status,
        Integer total,
        Integer successCount,
        Integer failedCount,
        String errorReport,
        UUID createdBy,
        Instant createdAt,
        Instant finishedAt) {

    public static ImportJobResponse from(IamImportJob job) {
        return new ImportJobResponse(
                job.getId(),
                job.getType(),
                job.getFileRef(),
                job.getStatus(),
                job.getTotal(),
                job.getSuccessCount(),
                job.getFailedCount(),
                job.getErrorReport(),
                job.getCreatedBy(),
                job.getCreatedAt(),
                job.getFinishedAt());
    }
}
