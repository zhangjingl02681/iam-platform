package com.enterprise.iam.identity.repo;

import com.enterprise.iam.identity.domain.IamAttributeDef;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamAttributeDefRepository extends JpaRepository<IamAttributeDef, UUID> {

    Optional<IamAttributeDef> findByCode(String code);
}
