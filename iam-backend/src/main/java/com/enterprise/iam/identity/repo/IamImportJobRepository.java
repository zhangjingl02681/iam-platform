package com.enterprise.iam.identity.repo;

import com.enterprise.iam.identity.domain.IamImportJob;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamImportJobRepository extends JpaRepository<IamImportJob, UUID> {
}
