package com.enterprise.iam.oidc;

import com.enterprise.iam.application.domain.IamApplication;
import com.enterprise.iam.application.domain.IamOAuthClient;
import com.enterprise.iam.application.domain.OAuthClientStatus;
import com.enterprise.iam.application.domain.OAuthClientType;
import com.enterprise.iam.application.repo.IamApplicationRepository;
import com.enterprise.iam.application.repo.IamOAuthClientRepository;
import com.enterprise.iam.config.IamProperties;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class JpaRegisteredClientRepository implements RegisteredClientRepository {

    private final IamOAuthClientRepository clientRepository;
    private final IamApplicationRepository applicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final IamProperties properties;

    public JpaRegisteredClientRepository(
            IamOAuthClientRepository clientRepository,
            IamApplicationRepository applicationRepository,
            PasswordEncoder passwordEncoder,
            IamProperties properties) {
        this.clientRepository = clientRepository;
        this.applicationRepository = applicationRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void save(RegisteredClient registeredClient) {
        IamOAuthClient entity = clientRepository.findByClientId(registeredClient.getClientId())
                .or(() -> {
                    try {
                        return clientRepository.findById(UUID.fromString(registeredClient.getId()));
                    } catch (IllegalArgumentException ex) {
                        return java.util.Optional.empty();
                    }
                })
                .orElseGet(IamOAuthClient::new);
        if (entity.getId() == null) {
            try {
                entity.setId(UUID.fromString(registeredClient.getId()));
            } catch (IllegalArgumentException ex) {
                entity.setId(UUID.randomUUID());
            }
        }
        if (entity.getAppId() == null) {
            entity.setAppId(ensureApplication().getId());
        }
        entity.setClientId(registeredClient.getClientId());
        entity.setClientType(registeredClient.getClientAuthenticationMethods()
                .contains(ClientAuthenticationMethod.NONE)
                ? OAuthClientType.PUBLIC
                : OAuthClientType.CONFIDENTIAL);
        entity.setGrantTypes(TextValues.join(
                registeredClient.getAuthorizationGrantTypes().stream().map(AuthorizationGrantType::getValue).toList()));
        entity.setRedirectUris(TextValues.join(registeredClient.getRedirectUris()));
        entity.setPostLogoutRedirectUris(TextValues.join(registeredClient.getPostLogoutRedirectUris()));
        entity.setScopes(TextValues.join(registeredClient.getScopes()));
        entity.setPkceRequired(registeredClient.getClientSettings().isRequireProofKey());
        entity.setRequireConsent(registeredClient.getClientSettings().isRequireAuthorizationConsent());
        entity.setTokenEndpointAuth(registeredClient.getClientAuthenticationMethods().stream()
                .findFirst()
                .map(ClientAuthenticationMethod::getValue)
                .orElse(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue()));
        if (StringUtils.hasText(registeredClient.getClientSecret())) {
            String secret = registeredClient.getClientSecret();
            entity.setClientSecretHash(secret.startsWith("$argon2") ? secret : passwordEncoder.encode(secret));
        }
        entity.setStatus(OAuthClientStatus.ACTIVE);
        clientRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public RegisteredClient findById(String id) {
        try {
            return clientRepository.findById(UUID.fromString(id))
                    .filter(client -> client.getStatus() == OAuthClientStatus.ACTIVE)
                    .map(this::toRegisteredClient)
                    .orElse(null);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RegisteredClient findByClientId(String clientId) {
        return clientRepository.findByClientId(clientId)
                .filter(client -> client.getStatus() == OAuthClientStatus.ACTIVE)
                .map(this::toRegisteredClient)
                .orElse(null);
    }

    private RegisteredClient toRegisteredClient(IamOAuthClient entity) {
        TokenSettings tokenSettings = TokenSettings.builder()
                .authorizationCodeTimeToLive(properties.getOidc().getAuthorizationCodeTtl())
                .accessTokenTimeToLive(properties.getOidc().getAccessTokenTtl())
                .refreshTokenTimeToLive(properties.getOidc().getRefreshTokenTtl())
                .reuseRefreshTokens(false)
                .build();
        RegisteredClient.Builder builder = RegisteredClient.withId(entity.getId().toString())
                .clientId(entity.getClientId())
                .clientSecret(entity.getClientSecretHash())
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(entity.isPkceRequired())
                        .requireAuthorizationConsent(entity.isRequireConsent())
                        .build())
                .tokenSettings(tokenSettings);
        TextValues.split(entity.getGrantTypes()).forEach(value ->
                builder.authorizationGrantType(new AuthorizationGrantType(value)));
        TextValues.split(entity.getRedirectUris()).forEach(builder::redirectUri);
        TextValues.split(entity.getPostLogoutRedirectUris()).forEach(builder::postLogoutRedirectUri);
        TextValues.split(entity.getScopes()).forEach(builder::scope);
        String authMethod = StringUtils.hasText(entity.getTokenEndpointAuth())
                ? entity.getTokenEndpointAuth()
                : ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue();
        builder.clientAuthenticationMethod(new ClientAuthenticationMethod(authMethod));
        if (entity.getClientType() == OAuthClientType.PUBLIC) {
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
        }
        return builder.build();
    }

    private IamApplication ensureApplication() {
        return applicationRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No IAM application exists to attach an OAuth client"));
    }
}
