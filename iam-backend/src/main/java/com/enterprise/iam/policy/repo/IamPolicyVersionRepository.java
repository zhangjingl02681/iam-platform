package com.enterprise.iam.policy.repo;

import com.enterprise.iam.policy.domain.IamPolicyVersion;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamPolicyVersionRepository extends JpaRepository<IamPolicyVersion, UUID> {
}
