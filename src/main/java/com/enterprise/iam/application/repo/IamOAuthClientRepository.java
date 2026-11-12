package com.enterprise.iam.application.repo;

import com.enterprise.iam.application.domain.IamOAuthClient;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamOAuthClientRepository extends JpaRepository<IamOAuthClient, UUID> {

    Optional<IamOAuthClient> findByClientId(String clientId);
}
