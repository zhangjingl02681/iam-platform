package com.enterprise.iam.pam.repo;

import com.enterprise.iam.pam.domain.PamCheckout;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PamCheckoutRepository extends JpaRepository<PamCheckout, UUID> {

    List<PamCheckout> findByUserIdOrderByCheckedOutAtDesc(UUID userId);

    List<PamCheckout> findByRequestIdOrderByCheckedOutAtDesc(UUID requestId);

    Optional<PamCheckout> findFirstByRequestIdAndCheckedInAtIsNullOrderByCheckedOutAtDesc(UUID requestId);
}
