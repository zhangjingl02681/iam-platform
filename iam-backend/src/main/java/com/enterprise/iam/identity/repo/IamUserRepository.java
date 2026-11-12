package com.enterprise.iam.identity.repo;

import com.enterprise.iam.identity.domain.IamUser;
import com.enterprise.iam.identity.domain.UserStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamUserRepository extends JpaRepository<IamUser, UUID> {

    Optional<IamUser> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmployeeNo(String employeeNo);

    boolean existsByEmployeeNoAndIdNot(String employeeNo, UUID id);

    Page<IamUser> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    Page<IamUser> findByStatus(UserStatus status, Pageable pageable);

    Page<IamUser> findByUsernameContainingIgnoreCaseAndStatus(
            String username, UserStatus status, Pageable pageable);
}
