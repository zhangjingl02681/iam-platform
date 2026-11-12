package com.enterprise.iam.oidc.repo;

import com.enterprise.iam.oidc.domain.IamSigningKey;
import com.enterprise.iam.oidc.domain.SigningKeyStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamSigningKeyRepository extends JpaRepository<IamSigningKey, UUID> {

    Optional<IamSigningKey> findByKid(String kid);

    Optional<IamSigningKey> findByStatus(SigningKeyStatus status);

    List<IamSigningKey> findAllByOrderByCreatedAtDesc();

    List<IamSigningKey> findByStatusIn(List<SigningKeyStatus> statuses);
}
