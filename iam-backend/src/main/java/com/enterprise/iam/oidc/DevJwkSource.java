package com.enterprise.iam.oidc;

import com.enterprise.iam.config.IamProperties;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Loads or generates a dev RSA signing key. Production keys must come from a vault
 * and be pointed at {@code iam.oidc.jwk-location}; they must not live in the business
 * database.
 */
@Configuration
public class DevJwkSource {

    private static final Logger log = LoggerFactory.getLogger(DevJwkSource.class);

    @Bean
    public JWKSource<SecurityContext> jwkSource(IamProperties properties) throws Exception {
        Path path = Path.of(properties.getOidc().getJwkLocation()).toAbsolutePath();
        RSAKey rsaKey;
        if (Files.exists(path)) {
            JWKSet existing = JWKSet.parse(Files.readString(path));
            rsaKey = (RSAKey) existing.getKeys().getFirst();
            log.info("Loaded OIDC signing JWK from {}", path);
        } else {
            rsaKey = new RSAKeyGenerator(2048)
                    .keyID(UUID.randomUUID().toString())
                    .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
                    .generate();
            Files.createDirectories(path.getParent());
            Files.writeString(path, new JWKSet(List.of(rsaKey)).toString(false));
            log.warn(
                    "Generated a development signing JWK at {}. Do not use this key in production; load keys from a vault.",
                    path);
        }
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }
}
