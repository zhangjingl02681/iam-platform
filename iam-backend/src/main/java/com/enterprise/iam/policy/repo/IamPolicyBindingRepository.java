package com.enterprise.iam.policy.repo;

import com.enterprise.iam.policy.domain.IamPolicyBinding;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamPolicyBindingRepository extends JpaRepository<IamPolicyBinding, UUID> {

    List<IamPolicyBinding> findByPolicyIdOrderByCreatedAtAsc(UUID policyId);

    boolean existsByVersionIdAndTargetTypeAndTargetId(UUID versionId, String targetType, UUID targetId);
}
