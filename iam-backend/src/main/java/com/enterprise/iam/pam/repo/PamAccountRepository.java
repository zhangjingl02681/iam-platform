package com.enterprise.iam.pam.repo;

import com.enterprise.iam.pam.domain.PamAccount;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PamAccountRepository extends JpaRepository<PamAccount, UUID> {
}
