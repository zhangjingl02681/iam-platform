package com.enterprise.iam.session.repo;

import com.enterprise.iam.session.domain.IamRefreshToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamRefreshTokenRepository extends JpaRepository<IamRefreshToken, UUID> {

    Optional<IamRefreshToken> findByTokenHash(String tokenHash);

    List<IamRefreshToken> findByAuthorizationId(UUID authorizationId);

    List<IamRefreshToken> findByFamilyId(UUID familyId);

    List<IamRefreshToken> findBySessionId(UUID sessionId);

    Optional<IamRefreshToken> findFirstByAuthorizationIdAndRevokedAtIsNullOrderByCreatedAtDesc(UUID authorizationId);
}

