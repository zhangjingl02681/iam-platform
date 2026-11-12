package com.enterprise.iam.session.repo;

import com.enterprise.iam.session.domain.IamRevocation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamRevocationRepository extends JpaRepository<IamRevocation, UUID> {
}
