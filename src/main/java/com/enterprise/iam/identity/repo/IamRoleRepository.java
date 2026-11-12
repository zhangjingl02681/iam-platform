package com.enterprise.iam.identity.repo;

import com.enterprise.iam.identity.domain.IamRole;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamRoleRepository extends JpaRepository<IamRole, UUID> {

    Optional<IamRole> findByCode(String code);
}
