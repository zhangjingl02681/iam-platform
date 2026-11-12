package com.enterprise.iam.policy.repo;

import com.enterprise.iam.policy.domain.IamPolicyBinding;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamPolicyBindingRepository extends JpaRepository<IamPolicyBinding, UUID> {
}
