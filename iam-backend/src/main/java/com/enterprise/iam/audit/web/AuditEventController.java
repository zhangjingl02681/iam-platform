package com.enterprise.iam.audit.web;

import com.enterprise.iam.audit.AuditQueryService;
import com.enterprise.iam.audit.domain.IamAuditEvent;
import com.enterprise.iam.audit.repo.IamAuditEventRepository;
import com.enterprise.iam.audit.web.dto.AuditEventResponse;
import com.enterprise.iam.common.exception.NotFoundException;
import com.enterprise.iam.common.web.PageResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-events")
@PreAuthorize("hasRole('ADMIN')")
public class AuditEventController {

    private final AuditQueryService auditQueryService;
    private final IamAuditEventRepository auditEventRepository;

    public AuditEventController(AuditQueryService auditQueryService, IamAuditEventRepository auditEventRepository) {
        this.auditQueryService = auditQueryService;
        this.auditEventRepository = auditEventRepository;
    }

    @GetMapping
    public PageResponse<AuditEventResponse> search(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) UUID objectId,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(auditQueryService
                .search(from, to, actorId, action, eventType, objectType, objectId, pageable)
                .map(AuditEventResponse::from));
    }

    @GetMapping("/{id}")
    public AuditEventResponse get(@PathVariable UUID id) {
        IamAuditEvent event = auditEventRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("audit_event_not_found", "Audit event not found"));
        return AuditEventResponse.from(event);
    }
}
