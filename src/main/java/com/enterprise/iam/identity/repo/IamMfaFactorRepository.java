package com.enterprise.iam.identity.repo;

import com.enterprise.iam.identity.domain.IamMfaFactor;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamMfaFactorRepository extends JpaRepository<IamMfaFactor, UUID> {
}
