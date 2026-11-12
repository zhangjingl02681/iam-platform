package com.enterprise.iam.vault;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSecretVaultTests {

    @TempDir
    Path tempDir;

    @Test
    void storesAndLoadsSecretsUnderTheVaultDirectoryAndRejectsTraversal() throws Exception {
        FileSecretVault vault = new FileSecretVault(tempDir);
        vault.store("vault:pam/abc", "super-secret");
        assertThat(vault.load("vault:pam/abc")).hasValue("super-secret");
        Path stored = Files.walk(tempDir)
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow();
        assertThat(stored.toAbsolutePath().startsWith(tempDir.toAbsolutePath())).isTrue();
        assertThat(Files.readString(stored)).isEqualTo("super-secret");

        assertThatThrownBy(() -> vault.store("../escape", "nope")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> vault.load("vault:../../../etc/passwd")).isInstanceOf(IllegalArgumentException.class);
    }
}
