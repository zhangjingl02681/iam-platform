package com.enterprise.iam.identity.repo;

import com.enterprise.iam.identity.domain.IamPasswordHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamPasswordHistoryRepository extends JpaRepository<IamPasswordHistory, UUID> {

    List<IamPasswordHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
