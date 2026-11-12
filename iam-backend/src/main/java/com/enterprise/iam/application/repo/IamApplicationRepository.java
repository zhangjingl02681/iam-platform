package com.enterprise.iam.application.repo;

import com.enterprise.iam.application.domain.ApplicationStatus;
import com.enterprise.iam.application.domain.IamApplication;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IamApplicationRepository extends JpaRepository<IamApplication, UUID> {

    Optional<IamApplication> findByCode(String code);

    boolean existsByCode(String code);

    Page<IamApplication> findByStatus(ApplicationStatus status, Pageable pageable);

    List<IamApplication> findByStatusAndLaunchUrlIsNotNullOrderByNameAsc(ApplicationStatus status);

    @Query(
            """
            select a from IamApplication a
            where lower(a.code) like lower(concat('%', :query, '%'))
               or lower(a.name) like lower(concat('%', :query, '%'))
            """)
    Page<IamApplication> searchByQuery(@Param("query") String query, Pageable pageable);

    @Query(
            """
            select a from IamApplication a
            where a.status = :status
              and (lower(a.code) like lower(concat('%', :query, '%'))
                or lower(a.name) like lower(concat('%', :query, '%')))
            """)
    Page<IamApplication> searchByQueryAndStatus(
            @Param("query") String query, @Param("status") ApplicationStatus status, Pageable pageable);
}
