package com.enterprise.iam.identity.repo;

import com.enterprise.iam.identity.domain.IamUserRole;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamUserRoleRepository extends JpaRepository<IamUserRole, UUID> {

    List<IamUserRole> findByUserId(UUID userId);

    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);
}
