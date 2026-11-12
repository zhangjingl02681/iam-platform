package com.enterprise.iam.pam.repo;

import com.enterprise.iam.pam.domain.PamCheckout;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PamCheckoutRepository extends JpaRepository<PamCheckout, UUID> {
}
