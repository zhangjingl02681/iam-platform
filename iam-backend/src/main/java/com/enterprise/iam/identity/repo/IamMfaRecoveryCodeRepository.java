package com.enterprise.iam.identity.repo;

import com.enterprise.iam.identity.domain.IamMfaRecoveryCode;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamMfaRecoveryCodeRepository extends JpaRepository<IamMfaRecoveryCode, UUID> {

    List<IamMfaRecoveryCode> findByUserIdAndUsedAtIsNull(UUID userId);
}
