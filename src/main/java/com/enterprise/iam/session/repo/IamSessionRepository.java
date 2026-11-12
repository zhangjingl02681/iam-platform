package com.enterprise.iam.session.repo;

import com.enterprise.iam.session.domain.IamSession;
import com.enterprise.iam.session.domain.SessionStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamSessionRepository extends JpaRepository<IamSession, UUID> {

    List<IamSession> findByUserIdOrderByLastSeenAtDesc(UUID userId);

    Page<IamSession> findByUserId(UUID userId, Pageable pageable);

    Page<IamSession> findByUserIdAndStatus(UUID userId, SessionStatus status, Pageable pageable);

    Page<IamSession> findByStatus(SessionStatus status, Pageable pageable);
}
