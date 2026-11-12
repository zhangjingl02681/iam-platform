package com.enterprise.iam.oidc.repo;

import com.enterprise.iam.oidc.domain.IamOAuthAuthorization;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamOAuthAuthorizationRepository extends JpaRepository<IamOAuthAuthorization, UUID> {

    Optional<IamOAuthAuthorization> findByAccessTokenHash(String accessTokenHash);

    Optional<IamOAuthAuthorization> findByIdTokenHash(String idTokenHash);

    List<IamOAuthAuthorization> findBySessionId(UUID sessionId);

    Optional<IamOAuthAuthorization> findByState(String state);
}
