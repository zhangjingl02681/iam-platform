package com.enterprise.iam.common;

import com.enterprise.iam.auth.IamPrincipal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<IamPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof IamPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static Optional<UUID> currentUserId() {
        return currentPrincipal().map(IamPrincipal::getUserId);
    }

    public static UUID requireUserId() {
        return currentUserId().orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }
}
