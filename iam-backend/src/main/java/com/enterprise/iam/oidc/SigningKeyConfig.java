package com.enterprise.iam.oidc;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class SigningKeyConfig {

    @Bean
    public JWKSource<SecurityContext> jwkSource(SigningKeyService signingKeyService) {
        return (selector, context) -> selector.select(signingKeyService.jwkSetForValidation());
    }

    @Bean
    public JwtEncoder jwtEncoder(SigningKeyService signingKeyService) {
        return new JwtEncoder() {
            @Override
            public Jwt encode(JwtEncoderParameters parameters) {
                JWKSet current = new JWKSet(signingKeyService.currentSigningKey());
                return new NimbusJwtEncoder(new ImmutableJWKSet<>(current)).encode(parameters);
            }
        };
    }

    @Bean
    public ApplicationRunner signingKeyInitializer(SigningKeyService signingKeyService) {
        return args -> signingKeyService.ensureCurrentKey();
    }
}
