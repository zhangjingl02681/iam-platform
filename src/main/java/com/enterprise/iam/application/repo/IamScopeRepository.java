package com.enterprise.iam.application.repo;

import com.enterprise.iam.application.domain.IamScope;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamScopeRepository extends JpaRepository<IamScope, String> {
}
