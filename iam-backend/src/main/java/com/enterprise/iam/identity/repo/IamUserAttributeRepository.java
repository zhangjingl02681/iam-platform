package com.enterprise.iam.identity.repo;

import com.enterprise.iam.identity.domain.IamUserAttribute;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamUserAttributeRepository extends JpaRepository<IamUserAttribute, UUID> {

    List<IamUserAttribute> findByUserIdOrderByAttrCodeAsc(UUID userId);

    Optional<IamUserAttribute> findByUserIdAndAttrCode(UUID userId, String attrCode);

    void deleteByUserId(UUID userId);
}
