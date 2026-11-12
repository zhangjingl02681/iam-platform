package com.enterprise.iam.policy.document;

import java.util.List;

/**
 * JSON policy document evaluated by the built-in PDP (not OPA or Cedar).
 *
 * <pre>
 * {
 *   "effect": "allow" | "deny",
 *   "actions": ["app:launch", "user:read"],
 *   "resources": [{"type": "application", "id": "*", "code": "finance-app"}],
 *   "when": {
 *     "roles": ["finance"],
 *     "orgCodes": ["eng"],
 *     "attributes": {"title": "Engineer"}
 *   }
 * }
 * </pre>
 *
 * Deny overrides allow. If nothing matches, the decision is deny. Unpublished
 * versions are ignored. A policy takes effect only after a published version is bound
 * to a target (application or org).
 */
public record PolicyDocument(
        String effect,
        List<String> actions,
        List<PolicyResource> resources,
        PolicyWhen when) {

    public boolean deny() {
        return "deny".equalsIgnoreCase(effect);
    }

    public boolean allow() {
        return "allow".equalsIgnoreCase(effect);
    }
}
