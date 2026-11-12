package com.enterprise.iam.identity.repo;

import com.enterprise.iam.identity.domain.IamPosition;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamPositionRepository extends JpaRepository<IamPosition, UUID> {

    boolean existsByCode(String code);
}
