package com.enterprise.iam.identity.repo;

import com.enterprise.iam.identity.domain.IamUserAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamUserAssignmentRepository extends JpaRepository<IamUserAssignment, UUID> {

    List<IamUserAssignment> findByUserId(UUID userId);

    Optional<IamUserAssignment> findByUserIdAndPrimaryIsTrue(UUID userId);
}
