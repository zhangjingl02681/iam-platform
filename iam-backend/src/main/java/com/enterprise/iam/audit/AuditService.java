package com.enterprise.iam.audit;

import com.enterprise.iam.audit.domain.IamAuditEvent;
import com.enterprise.iam.audit.repo.IamAuditEventRepository;
import com.enterprise.iam.common.SecurityUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final IamAuditEventRepository repository;
    private final Object lock = new Object();

    public AuditService(IamAuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public IamAuditEvent record(
            String eventType,
            String action,
            String objectType,
            UUID objectId,
            String result,
            Map<String, Object> detail) {
        IamAuditEvent event = new IamAuditEvent();
        event.setOccurredAt(Instant.now());
        event.setEventType(eventType);
        event.setActorType("user");
        event.setActorId(SecurityUtils.currentUserId().orElse(null));
        event.setObjectType(objectType);
        event.setObjectId(objectId);
        event.setAction(action);
        event.setResult(result);
        event.setDetail(detail == null ? null : new LinkedHashMap<>(detail));
        return append(event);
    }

    @Transactional
    public IamAuditEvent append(IamAuditEvent event) {
        if (event.getOccurredAt() == null) {
            event.setOccurredAt(Instant.now());
        }
        if (event.getResult() == null) {
            event.setResult("success");
        }
        synchronized (lock) {
            String prevHash = repository.findTopByOrderByOccurredAtDescIdDesc()
                    .map(IamAuditEvent::getEventHash)
                    .orElse(null);
            event.setPrevHash(prevHash);
            event.setEventHash(computeHash(event, prevHash));
            return repository.save(event);
        }
    }

    static String computeHash(IamAuditEvent event, String prevHash) {
        String canonical = String.join("|",
                String.valueOf(event.getOccurredAt()),
                nullToEmpty(event.getEventType()),
                nullToEmpty(event.getActorType()),
                event.getActorId() == null ? "" : event.getActorId().toString(),
                nullToEmpty(event.getObjectType()),
                event.getObjectId() == null ? "" : event.getObjectId().toString(),
                nullToEmpty(event.getAction()),
                nullToEmpty(event.getResult()),
                String.valueOf(event.getDetail()),
                nullToEmpty(prevHash));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
