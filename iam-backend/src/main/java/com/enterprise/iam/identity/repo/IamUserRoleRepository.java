package com.enterprise.iam.identity.repo;

import com.enterprise.iam.identity.domain.IamUserRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamUserRoleRepository extends JpaRepository<IamUserRole, UUID> {

    List<IamUserRole> findByUserId(UUID userId);

    List<IamUserRole> findByRoleId(UUID roleId);

    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);

    Optional<IamUserRole> findByUserIdAndRoleId(UUID userId, UUID roleId);

    void deleteByUserIdAndRoleId(UUID userId, UUID roleId);

    long countByRoleId(UUID roleId);
}
