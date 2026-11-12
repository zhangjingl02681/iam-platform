package com.enterprise.iam.application.repo;

import com.enterprise.iam.application.domain.EntitlementSubjectType;
import com.enterprise.iam.application.domain.IamApplicationEntitlement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamApplicationEntitlementRepository extends JpaRepository<IamApplicationEntitlement, UUID> {

    List<IamApplicationEntitlement> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);

    List<IamApplicationEntitlement> findByApplicationIdIn(Collection<UUID> applicationIds);

    boolean existsByApplicationId(UUID applicationId);

    boolean existsByApplicationIdAndSubjectTypeAndSubjectId(
            UUID applicationId, EntitlementSubjectType subjectType, UUID subjectId);

    Optional<IamApplicationEntitlement> findByApplicationIdAndId(UUID applicationId, UUID id);
}
