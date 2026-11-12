package com.enterprise.iam.pam.web.dto;

import com.enterprise.iam.pam.domain.PamCheckout;
import java.time.Instant;
import java.util.UUID;

public record PamCheckoutResponse(
        UUID id,
        UUID requestId,
        UUID accountId,
        UUID userId,
        Instant checkedOutAt,
        Instant expiresAt,
        Instant checkedInAt,
        String status) {

    public static PamCheckoutResponse from(PamCheckout checkout, Instant now) {
        return new PamCheckoutResponse(
                checkout.getId(),
                checkout.getRequestId(),
                checkout.getAccountId(),
                checkout.getUserId(),
                checkout.getCheckedOutAt(),
                checkout.getExpiresAt(),
                checkout.getCheckedInAt(),
                statusOf(checkout, now));
    }

    public static String statusOf(PamCheckout checkout, Instant now) {
        if (checkout.getCheckedInAt() != null) {
            return "CHECKED_IN";
        }
        if (checkout.getExpiresAt() != null && !checkout.getExpiresAt().isAfter(now)) {
            return "EXPIRED";
        }
        return "ACTIVE";
    }
}
