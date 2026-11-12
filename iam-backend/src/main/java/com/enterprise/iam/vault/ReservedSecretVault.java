package com.enterprise.iam.vault;

import java.util.Optional;

/**
 * Placeholder for a production HSM / cloud KMS. This increment does not talk to
 * any external key management service.
 */
public class ReservedSecretVault implements SecretVault {

    @Override
    public void store(String vaultRef, String secret) {
        throw new UnsupportedOperationException(
                "Remote vault is reserved. Configure iam.vault.type=file for development, or provide a production KMS adapter.");
    }

    @Override
    public Optional<String> load(String vaultRef) {
        throw new UnsupportedOperationException(
                "Remote vault is reserved. Configure iam.vault.type=file for development, or provide a production KMS adapter.");
    }

    @Override
    public void delete(String vaultRef) {
        throw new UnsupportedOperationException(
                "Remote vault is reserved. Configure iam.vault.type=file for development, or provide a production KMS adapter.");
    }
}
