/**
 * Spring Authorization Server (OIDC / OAuth 2.1) integration.
 *
 * <p>Protocol cryptography is provided by Spring Security. Authorization codes,
 * refresh tokens and client secrets are stored as hashes in the business tables.
 * Signing keys are loaded from {@code iam.oidc.jwk-location} (a generated dev JWK
 * by default) and must come from a vault in production.
 */
package com.enterprise.iam.oidc;
