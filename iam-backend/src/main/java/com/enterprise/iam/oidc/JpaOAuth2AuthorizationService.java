package com.enterprise.iam.oidc;

import com.enterprise.iam.auth.IamPrincipal;
import com.enterprise.iam.oidc.domain.IamOAuthAuthorization;
import com.enterprise.iam.oidc.repo.IamOAuthAuthorizationRepository;
import com.enterprise.iam.session.IamLoginSessionService;
import com.enterprise.iam.session.domain.IamAuthorizationCode;
import com.enterprise.iam.session.domain.IamRefreshToken;
import com.enterprise.iam.session.repo.IamAuthorizationCodeRepository;
import com.enterprise.iam.session.repo.IamRefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class JpaOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private static final OAuth2TokenType AUTHORIZATION_CODE_TOKEN_TYPE =
            new OAuth2TokenType(OAuth2ParameterNames.CODE);
    private static final OAuth2TokenType ID_TOKEN_TOKEN_TYPE = new OAuth2TokenType(OidcParameterNames.ID_TOKEN);


    private final IamOAuthAuthorizationRepository authorizationRepository;
    private final IamAuthorizationCodeRepository authorizationCodeRepository;
    private final IamRefreshTokenRepository refreshTokenRepository;
    private final RegisteredClientRepository registeredClientRepository;
    private final UserDetailsService userDetailsService;
    private final IamLoginSessionService loginSessionService;

    public JpaOAuth2AuthorizationService(
            IamOAuthAuthorizationRepository authorizationRepository,
            IamAuthorizationCodeRepository authorizationCodeRepository,
            IamRefreshTokenRepository refreshTokenRepository,
            RegisteredClientRepository registeredClientRepository,
            UserDetailsService userDetailsService,
            IamLoginSessionService loginSessionService) {
        this.authorizationRepository = authorizationRepository;
        this.authorizationCodeRepository = authorizationCodeRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.registeredClientRepository = registeredClientRepository;
        this.userDetailsService = userDetailsService;
        this.loginSessionService = loginSessionService;
    }

    @Override
    @Transactional
    public void save(OAuth2Authorization authorization) {
        UUID id = UUID.fromString(authorization.getId());
        IamOAuthAuthorization entity = authorizationRepository.findById(id).orElseGet(() -> {
            IamOAuthAuthorization created = new IamOAuthAuthorization();
            created.setId(id);
            return created;
        });
        entity.setRegisteredClientId(authorization.getRegisteredClientId());
        entity.setPrincipalName(authorization.getPrincipalName());
        entity.setGrantType(authorization.getAuthorizationGrantType().getValue());
        entity.setAuthorizedScopes(TextValues.join(authorization.getAuthorizedScopes()));
        entity.setState(authorization.getAttribute(OAuth2ParameterNames.STATE));
        OAuth2AuthorizationRequest request = authorization.getAttribute(OAuth2AuthorizationRequest.class.getName());
        if (request != null) {
            entity.setAuthorizationRequest(AuthorizationRequestMapper.toMap(request));
        }
        IamPrincipal principal = extractPrincipal(authorization);
        if (principal != null) {
            entity.setUserId(principal.getUserId());
        }
        if (entity.getSessionId() == null) {
            currentRequest().flatMap(loginSessionService::currentSessionId).ifPresent(entity::setSessionId);
        }

        saveAccessToken(entity, authorization.getAccessToken());
        saveIdToken(entity, authorization.getToken(OidcIdToken.class));
        saveAuthorizationCode(entity, authorization);
        saveRefreshToken(entity, authorization);
        authorizationRepository.save(entity);
    }

    @Override
    @Transactional
    public void remove(OAuth2Authorization authorization) {
        UUID id = UUID.fromString(authorization.getId());
        Instant now = Instant.now();
        authorizationCodeRepository.findByAuthorizationId(id).ifPresent(code -> {
            if (code.getConsumedAt() == null) {
                code.setConsumedAt(now);
                authorizationCodeRepository.save(code);
            }
        });
        refreshTokenRepository.findByAuthorizationId(id).forEach(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(now);
                refreshTokenRepository.save(token);
            }
        });
        authorizationRepository.findById(id).ifPresent(entity -> {
            Map<String, Object> accessMeta = copy(entity.getAccessTokenMetadata());
            accessMeta.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, true);
            entity.setAccessTokenMetadata(accessMeta);
            authorizationRepository.save(entity);
        });
    }

    @Override
    @Transactional
    public OAuth2Authorization findById(String id) {
        try {
            return authorizationRepository.findById(UUID.fromString(id))
                    .map(entity -> toAuthorization(entity, null, null))
                    .orElse(null);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    @Transactional
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        if (token == null) {
            return null;
        }
        String hash = TokenHashes.sha256(token);
        if (tokenType == null || OAuth2ParameterNames.STATE.equals(tokenType.getValue())) {
            var found = authorizationRepository.findByState(token);
            if (found.isPresent()) {
                return toAuthorization(found.get(), null, null);
            }
            if (tokenType != null) {
                return null;
            }
        }
        if (tokenType == null || AUTHORIZATION_CODE_TOKEN_TYPE.equals(tokenType)) {
            var found = authorizationCodeRepository.findByCodeHash(hash);
            if (found.isPresent() && found.get().getAuthorizationId() != null) {
                return authorizationRepository.findById(found.get().getAuthorizationId())
                        .map(entity -> toAuthorization(entity, token, AUTHORIZATION_CODE_TOKEN_TYPE))
                        .orElse(null);
            }
            if (AUTHORIZATION_CODE_TOKEN_TYPE.equals(tokenType)) {
                return null;
            }
        }
        if (tokenType == null || OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
            var found = refreshTokenRepository.findByTokenHash(hash);
            if (found.isPresent()) {
                IamRefreshToken refresh = found.get();
                if (refresh.getRevokedAt() != null) {
                    loginSessionService.revokeFamily(refresh.getFamilyId(), "refresh_token_reuse");
                    return null;
                }
                if (refresh.getAuthorizationId() != null) {
                    return authorizationRepository.findById(refresh.getAuthorizationId())
                            .map(entity -> toAuthorization(entity, token, OAuth2TokenType.REFRESH_TOKEN))
                            .orElse(null);
                }
            }
            if (OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
                return null;
            }
        }
        if (tokenType == null || OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)) {
            var found = authorizationRepository.findByAccessTokenHash(hash);
            if (found.isPresent()) {
                return toAuthorization(found.get(), token, OAuth2TokenType.ACCESS_TOKEN);
            }
            if (OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)) {
                return null;
            }
        }
        if (tokenType == null || OidcParameterNames.ID_TOKEN.equals(tokenType.getValue())) {
            return authorizationRepository.findByIdTokenHash(hash)
                    .map(entity -> toAuthorization(entity, token, tokenType))
                    .orElse(null);
        }
        return null;
    }

    private void saveAccessToken(
            IamOAuthAuthorization entity, OAuth2Authorization.Token<OAuth2AccessToken> token) {
        if (token == null) {
            return;
        }
        OAuth2AccessToken access = token.getToken();
        entity.setAccessTokenHash(TokenHashes.sha256(access.getTokenValue()));
        entity.setAccessTokenIssuedAt(access.getIssuedAt());
        entity.setAccessTokenExpiresAt(access.getExpiresAt());
        entity.setAccessTokenScopes(TextValues.join(access.getScopes()));
        entity.setAccessTokenType(access.getTokenType() != null ? access.getTokenType().getValue() : "Bearer");
        entity.setAccessTokenMetadata(copy(token.getMetadata()));
    }

    private void saveIdToken(IamOAuthAuthorization entity, OAuth2Authorization.Token<OidcIdToken> token) {
        if (token == null) {
            return;
        }
        OidcIdToken idToken = token.getToken();
        entity.setIdTokenHash(TokenHashes.sha256(idToken.getTokenValue()));
        entity.setIdTokenIssuedAt(idToken.getIssuedAt());
        entity.setIdTokenExpiresAt(idToken.getExpiresAt());
        Map<String, Object> claims = token.getClaims() != null ? token.getClaims() : idToken.getClaims();
        entity.setIdTokenClaims(claims == null ? null : new HashMap<>(claims));
        entity.setIdTokenMetadata(copy(token.getMetadata()));
    }

    private void saveAuthorizationCode(IamOAuthAuthorization entity, OAuth2Authorization authorization) {
        OAuth2Authorization.Token<OAuth2AuthorizationCode> token = authorization.getToken(OAuth2AuthorizationCode.class);
        if (token == null) {
            return;
        }
        OAuth2AuthorizationCode code = token.getToken();
        String hash = TokenHashes.sha256(code.getTokenValue());
        IamAuthorizationCode row = authorizationCodeRepository.findByAuthorizationId(entity.getId())
                .or(() -> authorizationCodeRepository.findByCodeHash(hash))
                .orElseGet(IamAuthorizationCode::new);
        row.setAuthorizationId(entity.getId());
        row.setCodeHash(hash);
        row.setClientId(clientIdOf(authorization));
        row.setUserId(entity.getUserId());
        row.setSessionId(entity.getSessionId());
        OAuth2AuthorizationRequest request = authorization.getAttribute(OAuth2AuthorizationRequest.class.getName());
        if (request != null) {
            row.setRedirectUri(request.getRedirectUri());
            Object challenge = request.getAdditionalParameters().get(PkceParameterNames.CODE_CHALLENGE);
            Object method = request.getAdditionalParameters().get(PkceParameterNames.CODE_CHALLENGE_METHOD);
            row.setCodeChallenge(challenge == null ? null : challenge.toString());
            row.setChallengeMethod(method == null ? null : method.toString());
            Object nonce = request.getAdditionalParameters().get(OidcParameterNames.NONCE);
            row.setNonce(nonce == null ? null : nonce.toString());
        } else if (row.getRedirectUri() == null) {
            row.setRedirectUri("");
        }
        row.setScope(entity.getAuthorizedScopes());
        row.setExpiresAt(code.getExpiresAt());
        if (token.isInvalidated() && row.getConsumedAt() == null) {
            row.setConsumedAt(Instant.now());
        }
        if (entity.getUserId() == null) {
            return;
        }
        authorizationCodeRepository.save(row);
    }

    private void saveRefreshToken(IamOAuthAuthorization entity, OAuth2Authorization authorization) {
        OAuth2Authorization.Token<OAuth2RefreshToken> token = authorization.getRefreshToken();
        if (token == null) {
            return;
        }
        OAuth2RefreshToken refresh = token.getToken();
        String hash = TokenHashes.sha256(refresh.getTokenValue());
        IamRefreshToken existingActive = refreshTokenRepository
                .findFirstByAuthorizationIdAndRevokedAtIsNullOrderByCreatedAtDesc(entity.getId())
                .orElse(null);
        if (existingActive != null && hash.equals(existingActive.getTokenHash())) {
            if (token.isInvalidated() && existingActive.getRevokedAt() == null) {
                existingActive.setRevokedAt(Instant.now());
                refreshTokenRepository.save(existingActive);
            }
            return;
        }
        UUID familyId = entity.getRefreshFamilyId();
        if (familyId == null && existingActive != null) {
            familyId = existingActive.getFamilyId();
        }
        if (familyId == null) {
            familyId = UUID.randomUUID();
        }
        if (existingActive != null) {
            existingActive.setRevokedAt(Instant.now());
            refreshTokenRepository.save(existingActive);
        }
        IamRefreshToken row = new IamRefreshToken();
        row.setAuthorizationId(entity.getId());
        row.setTokenHash(hash);
        row.setFamilyId(familyId);
        row.setUserId(entity.getUserId());
        row.setClientId(clientIdOf(authorization));
        row.setSessionId(entity.getSessionId());
        row.setScope(entity.getAuthorizedScopes());
        row.setExpiresAt(refresh.getExpiresAt());
        if (token.isInvalidated()) {
            row.setRevokedAt(Instant.now());
        }
        if (entity.getUserId() == null) {
            return;
        }
        row = refreshTokenRepository.save(row);
        if (existingActive != null) {
            existingActive.setReplacedBy(row.getId());
            refreshTokenRepository.save(existingActive);
        }
        entity.setRefreshFamilyId(familyId);
    }

    private OAuth2Authorization toAuthorization(
            IamOAuthAuthorization entity, String presentedToken, OAuth2TokenType presentedType) {
        RegisteredClient client = registeredClientRepository.findById(entity.getRegisteredClientId());
        if (client == null) {
            return null;
        }
        OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(client)
                .id(entity.getId().toString())
                .principalName(entity.getPrincipalName())
                .authorizationGrantType(new AuthorizationGrantType(entity.getGrantType()))
                .authorizedScopes(TextValues.split(entity.getAuthorizedScopes()));
        if (entity.getState() != null) {
            builder.attribute(OAuth2ParameterNames.STATE, entity.getState());
        }
        OAuth2AuthorizationRequest request = AuthorizationRequestMapper.fromMap(entity.getAuthorizationRequest());
        if (request != null) {
            builder.attribute(OAuth2AuthorizationRequest.class.getName(), request);
        }
        if (entity.getUserId() != null) {
            var details = userDetailsService.loadUserByUsername(entity.getPrincipalName());
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    details, null, details.getAuthorities());
            builder.attribute(Principal.class.getName(), authentication);
        }
        authorizationCodeRepository.findByAuthorizationId(entity.getId()).ifPresent(row -> {
            String value = matches(presentedType, AUTHORIZATION_CODE_TOKEN_TYPE) ? presentedToken : "hashed:" + row.getCodeHash();
            OAuth2AuthorizationCode code = new OAuth2AuthorizationCode(
                    value, row.getCreatedAt() != null ? row.getCreatedAt() : Instant.now(), row.getExpiresAt());
            builder.token(code, metadata -> {
                metadata.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, row.getConsumedAt() != null);
            });
        });
        if (entity.getAccessTokenHash() != null) {
            String value = matches(presentedType, OAuth2TokenType.ACCESS_TOKEN)
                    ? presentedToken
                    : "hashed:" + entity.getAccessTokenHash();
            OAuth2AccessToken.TokenType type = "DPoP".equalsIgnoreCase(entity.getAccessTokenType())
                    ? new OAuth2AccessToken.TokenType("DPoP")
                    : OAuth2AccessToken.TokenType.BEARER;
            OAuth2AccessToken access = new OAuth2AccessToken(
                    type,
                    value,
                    entity.getAccessTokenIssuedAt(),
                    entity.getAccessTokenExpiresAt(),
                    TextValues.split(entity.getAccessTokenScopes()));
            builder.token(access, metadata -> metadata.putAll(ClaimMaps.restore(entity.getAccessTokenMetadata())));
        }
        if (entity.getIdTokenHash() != null && entity.getIdTokenClaims() != null) {
            String value = presentedType != null && OidcParameterNames.ID_TOKEN.equals(presentedType.getValue())
                    ? presentedToken
                    : "hashed:" + entity.getIdTokenHash();
            Map<String, Object> idTokenClaims = ClaimMaps.restore(entity.getIdTokenClaims());
            OidcIdToken idToken = new OidcIdToken(
                    value,
                    entity.getIdTokenIssuedAt(),
                    entity.getIdTokenExpiresAt(),
                    idTokenClaims);
            builder.token(idToken, metadata -> {
                metadata.putAll(ClaimMaps.restore(entity.getIdTokenMetadata()));
                metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, idTokenClaims);
            });
        }
        refreshTokenRepository.findFirstByAuthorizationIdAndRevokedAtIsNullOrderByCreatedAtDesc(entity.getId())
                .or(() -> refreshTokenRepository.findByAuthorizationId(entity.getId()).stream().findFirst())
                .ifPresent(row -> {
                    String value = matches(presentedType, OAuth2TokenType.REFRESH_TOKEN)
                            ? presentedToken
                            : "hashed:" + row.getTokenHash();
                    Instant issued = row.getCreatedAt() != null ? row.getCreatedAt() : Instant.now();
                    OAuth2RefreshToken refresh = new OAuth2RefreshToken(value, issued, row.getExpiresAt());
                    builder.token(refresh, metadata -> metadata.put(
                            OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, row.getRevokedAt() != null));
                });
        return builder.build();
    }

    private static boolean matches(OAuth2TokenType presented, OAuth2TokenType expected) {
        return presented != null && expected != null && expected.equals(presented);
    }

    private String clientIdOf(OAuth2Authorization authorization) {
        RegisteredClient client = registeredClientRepository.findById(authorization.getRegisteredClientId());
        return client != null ? client.getClientId() : authorization.getRegisteredClientId();
    }

    private IamPrincipal extractPrincipal(OAuth2Authorization authorization) {
        Object attribute = authorization.getAttribute(Principal.class.getName());
        if (attribute instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof IamPrincipal principal) {
            return principal;
        }
        if (attribute instanceof IamPrincipal principal) {
            return principal;
        }
        return null;
    }

    private static Map<String, Object> copy(Map<String, Object> source) {
        return source == null ? new HashMap<>() : new HashMap<>(source);
    }

    private static java.util.Optional<HttpServletRequest> currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            return java.util.Optional.of(servlet.getRequest());
        }
        return java.util.Optional.empty();
    }
}
