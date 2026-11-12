package com.enterprise.iam.pam.repo;

import com.enterprise.iam.pam.domain.PamTarget;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PamTargetRepository extends JpaRepository<PamTarget, UUID> {
}
