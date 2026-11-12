package com.enterprise.iam.audit.repo;

import com.enterprise.iam.audit.domain.IamAuditEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Append-only access to {@code iam_audit_event}. Do not add update or delete methods.
 */
public interface IamAuditEventRepository extends Repository<IamAuditEvent, UUID> {

    IamAuditEvent save(IamAuditEvent event);

    Optional<IamAuditEvent> findById(UUID id);

    Optional<IamAuditEvent> findTopByOrderByOccurredAtDescIdDesc();

    List<IamAuditEvent> findByObjectIdAndEventTypeOrderByOccurredAtAsc(UUID objectId, String eventType);

    long countByObjectIdAndEventType(UUID objectId, String eventType);

    long count();

}
