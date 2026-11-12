package com.enterprise.iam.policy.repo;

import com.enterprise.iam.policy.domain.IamPolicyVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamPolicyVersionRepository extends JpaRepository<IamPolicyVersion, UUID> {

    List<IamPolicyVersion> findByPolicyIdOrderByVersionAsc(UUID policyId);

    Optional<IamPolicyVersion> findTopByPolicyIdOrderByVersionDesc(UUID policyId);

    Optional<IamPolicyVersion> findByPolicyIdAndId(UUID policyId, UUID id);
}
