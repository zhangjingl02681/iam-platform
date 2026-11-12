package com.enterprise.iam.identity.repo;

import com.enterprise.iam.identity.domain.CredentialType;
import com.enterprise.iam.identity.domain.IamCredential;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamCredentialRepository extends JpaRepository<IamCredential, UUID> {

    Optional<IamCredential> findByUserIdAndType(UUID userId, CredentialType type);
}
