package com.enterprise.iam.identity.repo;

import com.enterprise.iam.identity.domain.IamOrg;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamOrgRepository extends JpaRepository<IamOrg, UUID> {

    boolean existsByCode(String code);

    Optional<IamOrg> findByCode(String code);

    List<IamOrg> findByParentIdOrderBySortOrderAscNameAsc(UUID parentId);

    List<IamOrg> findByParentIdIsNullOrderBySortOrderAscNameAsc();

    List<IamOrg> findByPathStartingWith(String pathPrefix);

    List<IamOrg> findAllByOrderByPathAsc();
}
