package com.enterprise.iam.oidc.domain;

import com.enterprise.iam.common.jpa.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "iam_oauth_authorization")
public class IamOAuthAuthorization extends AuditedEntity {

    @Column(name = "registered_client_id", nullable = false, length = 64)
    private String registeredClientId;

    @Column(name = "principal_name", nullable = false, length = 256)
    private String principalName;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "grant_type", nullable = false, length = 64)
    private String grantType;

    @Column(name = "authorized_scopes")
    private String authorizedScopes;

    @Column(name = "state", length = 256)
    private String state;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "authorization_request_json", columnDefinition = "jsonb")
    private Map<String, Object> authorizationRequest;

    @Column(name = "access_token_hash", length = 128)
    private String accessTokenHash;

    @Column(name = "access_token_issued_at")
    private Instant accessTokenIssuedAt;

    @Column(name = "access_token_expires_at")
    private Instant accessTokenExpiresAt;

    @Column(name = "access_token_scopes")
    private String accessTokenScopes;

    @Column(name = "access_token_type", length = 32)
    private String accessTokenType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "access_token_metadata_json", columnDefinition = "jsonb")
    private Map<String, Object> accessTokenMetadata;

    @Column(name = "id_token_hash", length = 128)
    private String idTokenHash;

    @Column(name = "id_token_issued_at")
    private Instant idTokenIssuedAt;

    @Column(name = "id_token_expires_at")
    private Instant idTokenExpiresAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "id_token_claims_json", columnDefinition = "jsonb")
    private Map<String, Object> idTokenClaims;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "id_token_metadata_json", columnDefinition = "jsonb")
    private Map<String, Object> idTokenMetadata;

    @Column(name = "refresh_family_id")
    private UUID refreshFamilyId;

    public String getRegisteredClientId() {
        return registeredClientId;
    }

    public void setRegisteredClientId(String registeredClientId) {
        this.registeredClientId = registeredClientId;
    }

    public String getPrincipalName() {
        return principalName;
    }

    public void setPrincipalName(String principalName) {
        this.principalName = principalName;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getAuthorizedScopes() {
        return authorizedScopes;
    }

    public void setAuthorizedScopes(String authorizedScopes) {
        this.authorizedScopes = authorizedScopes;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Map<String, Object> getAuthorizationRequest() {
        return authorizationRequest;
    }

    public void setAuthorizationRequest(Map<String, Object> authorizationRequest) {
        this.authorizationRequest = authorizationRequest;
    }

    public String getAccessTokenHash() {
        return accessTokenHash;
    }

    public void setAccessTokenHash(String accessTokenHash) {
        this.accessTokenHash = accessTokenHash;
    }

    public Instant getAccessTokenIssuedAt() {
        return accessTokenIssuedAt;
    }

    public void setAccessTokenIssuedAt(Instant accessTokenIssuedAt) {
        this.accessTokenIssuedAt = accessTokenIssuedAt;
    }

    public Instant getAccessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }

    public void setAccessTokenExpiresAt(Instant accessTokenExpiresAt) {
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }

    public String getAccessTokenScopes() {
        return accessTokenScopes;
    }

    public void setAccessTokenScopes(String accessTokenScopes) {
        this.accessTokenScopes = accessTokenScopes;
    }

    public String getAccessTokenType() {
        return accessTokenType;
    }

    public void setAccessTokenType(String accessTokenType) {
        this.accessTokenType = accessTokenType;
    }

    public Map<String, Object> getAccessTokenMetadata() {
        return accessTokenMetadata;
    }

    public void setAccessTokenMetadata(Map<String, Object> accessTokenMetadata) {
        this.accessTokenMetadata = accessTokenMetadata;
    }

    public String getIdTokenHash() {
        return idTokenHash;
    }

    public void setIdTokenHash(String idTokenHash) {
        this.idTokenHash = idTokenHash;
    }

    public Instant getIdTokenIssuedAt() {
        return idTokenIssuedAt;
    }

    public void setIdTokenIssuedAt(Instant idTokenIssuedAt) {
        this.idTokenIssuedAt = idTokenIssuedAt;
    }

    public Instant getIdTokenExpiresAt() {
        return idTokenExpiresAt;
    }

    public void setIdTokenExpiresAt(Instant idTokenExpiresAt) {
        this.idTokenExpiresAt = idTokenExpiresAt;
    }

    public Map<String, Object> getIdTokenClaims() {
        return idTokenClaims;
    }

    public void setIdTokenClaims(Map<String, Object> idTokenClaims) {
        this.idTokenClaims = idTokenClaims;
    }

    public Map<String, Object> getIdTokenMetadata() {
        return idTokenMetadata;
    }

    public void setIdTokenMetadata(Map<String, Object> idTokenMetadata) {
        this.idTokenMetadata = idTokenMetadata;
    }

    public UUID getRefreshFamilyId() {
        return refreshFamilyId;
    }

    public void setRefreshFamilyId(UUID refreshFamilyId) {
        this.refreshFamilyId = refreshFamilyId;
    }
}
