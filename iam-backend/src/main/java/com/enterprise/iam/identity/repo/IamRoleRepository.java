package com.enterprise.iam.identity.repo;

import com.enterprise.iam.identity.domain.IamRole;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamRoleRepository extends JpaRepository<IamRole, UUID> {

    Optional<IamRole> findByCode(String code);

    boolean existsByCode(String code);

    List<IamRole> findByIdIn(Collection<UUID> ids);
}
