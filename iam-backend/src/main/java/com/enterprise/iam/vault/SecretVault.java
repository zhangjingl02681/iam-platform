package com.enterprise.iam.vault;

import java.util.Optional;

/**
 * Stores secrets identified by {@code vault_ref}. Implementations must never
 * write secrets into the business database.
 */
public interface SecretVault {

    void store(String vaultRef, String secret);

    Optional<String> load(String vaultRef);

    void delete(String vaultRef);
}
