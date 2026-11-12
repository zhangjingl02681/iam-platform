package com.enterprise.iam.pam.repo;

import com.enterprise.iam.pam.domain.PamRequest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PamRequestRepository extends JpaRepository<PamRequest, UUID> {
}
