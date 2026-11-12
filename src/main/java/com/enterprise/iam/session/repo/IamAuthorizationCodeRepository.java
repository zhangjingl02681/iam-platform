package com.enterprise.iam.session.repo;

import com.enterprise.iam.session.domain.IamAuthorizationCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamAuthorizationCodeRepository extends JpaRepository<IamAuthorizationCode, UUID> {

    Optional<IamAuthorizationCode> findByCodeHash(String codeHash);

    Optional<IamAuthorizationCode> findByAuthorizationId(UUID authorizationId);
}

