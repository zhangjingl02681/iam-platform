package com.enterprise.iam.policy.repo;

import com.enterprise.iam.policy.domain.IamPolicy;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamPolicyRepository extends JpaRepository<IamPolicy, UUID> {
}
