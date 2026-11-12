package com.enterprise.iam.vault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Development-grade vault that keeps secrets as files under a gitignored directory.
 * Production deployments should switch {@code iam.vault.type} to a reserved remote
 * implementation (HSM / cloud KMS) rather than using this directory.
 */
public class FileSecretVault implements SecretVault {

    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._:-]+");

    private final Path root;

    public FileSecretVault(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public void store(String vaultRef, String secret) {
        if (secret == null) {
            throw new IllegalArgumentException("secret is required");
        }
        Path path = resolve(vaultRef);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, secret, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store secret for " + vaultRef, ex);
        }
    }

    @Override
    public Optional<String> load(String vaultRef) {
        Path path = resolve(vaultRef);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load secret for " + vaultRef, ex);
        }
    }

    @Override
    public void delete(String vaultRef) {
        Path path = resolve(vaultRef);
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to delete secret for " + vaultRef, ex);
        }
    }

    public Path getRoot() {
        return root;
    }

    private Path resolve(String vaultRef) {
        if (vaultRef == null || vaultRef.isBlank()) {
            throw new IllegalArgumentException("vault_ref is required");
        }
        String trimmed = vaultRef.trim();
        String relative = trimmed;
        if (relative.toLowerCase(Locale.ROOT).startsWith("vault:")) {
            relative = relative.substring("vault:".length());
        }
        relative = relative.replace('\\', '/');
        if (relative.startsWith("/") || relative.contains("..")) {
            throw new IllegalArgumentException("Invalid vault_ref");
        }
        String[] parts = relative.split("/");
        StringBuilder fileName = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!SAFE.matcher(part).matches()) {
                throw new IllegalArgumentException("Invalid vault_ref");
            }
            if (fileName.length() > 0) {
                fileName.append("__");
            }
            fileName.append(part);
        }
        if (fileName.length() == 0) {
            throw new IllegalArgumentException("Invalid vault_ref");
        }
        Path resolved = root.resolve(fileName.toString()).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid vault_ref");
        }
        return resolved;
    }
}
