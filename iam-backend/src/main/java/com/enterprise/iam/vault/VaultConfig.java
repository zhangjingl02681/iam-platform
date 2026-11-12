package com.enterprise.iam.vault;

import com.enterprise.iam.config.IamProperties;
import java.nio.file.Path;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VaultConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public SecretVault secretVault(IamProperties properties) {
        String type = properties.getVault().getType();
        if (type == null || type.isBlank() || "file".equalsIgnoreCase(type)) {
            Path directory = Path.of(properties.getVault().getDirectory()).toAbsolutePath().normalize();
            return new FileSecretVault(directory);
        }
        if ("reserved".equalsIgnoreCase(type) || "remote".equalsIgnoreCase(type)) {
            return new ReservedSecretVault();
        }
        throw new IllegalStateException("Unsupported iam.vault.type: " + type);
    }
}
