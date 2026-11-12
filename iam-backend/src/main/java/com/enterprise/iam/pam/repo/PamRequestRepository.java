package com.enterprise.iam.pam.repo;

import com.enterprise.iam.pam.domain.PamRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PamRequestRepository extends JpaRepository<PamRequest, UUID> {

    List<PamRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);

    List<PamRequest> findAllByOrderByCreatedAtDesc();
}
