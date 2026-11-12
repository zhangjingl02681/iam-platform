package com.enterprise.iam.policy.web.dto;

import java.util.UUID;

public record PdpDecideResponse(
        String decision,
        String reason,
        UUID policyId,
        Integer version) {

    public static PdpDecideResponse allow(String reason, UUID policyId, int version) {
        return new PdpDecideResponse("allow", reason, policyId, version);
    }

    public static PdpDecideResponse deny(String reason, UUID policyId, Integer version) {
        return new PdpDecideResponse("deny", reason, policyId, version);
    }

    public static PdpDecideResponse defaultDeny() {
        return new PdpDecideResponse("deny", "default_deny", null, null);
    }

    public boolean allowed() {
        return "allow".equalsIgnoreCase(decision);
    }
}
