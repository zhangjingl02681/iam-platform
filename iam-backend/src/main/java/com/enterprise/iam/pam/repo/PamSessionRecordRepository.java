package com.enterprise.iam.pam.repo;

import com.enterprise.iam.pam.domain.PamSessionRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PamSessionRecordRepository extends JpaRepository<PamSessionRecord, UUID> {

    List<PamSessionRecord> findByCheckoutId(UUID checkoutId);
}
