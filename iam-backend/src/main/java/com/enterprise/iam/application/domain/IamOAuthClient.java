package com.enterprise.iam.application.domain;

import com.enterprise.iam.common.jpa.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "iam_oauth_client")
public class IamOAuthClient extends AuditedEntity {

    @Column(name = "client_id", nullable = false, unique = true, length = 128)
    private String clientId;

    @Column(name = "app_id", nullable = false)
    private UUID appId;

    @Column(name = "client_type", nullable = false, length = 32)
    private OAuthClientType clientType;

    @Column(name = "grant_types")
    private String grantTypes;

    @Column(name = "redirect_uris")
    private String redirectUris;

    @Column(name = "scopes")
    private String scopes;

    @Column(name = "pkce_required", nullable = false)
    private boolean pkceRequired = true;

    @Column(name = "token_endpoint_auth", length = 64)
    private String tokenEndpointAuth;

    @Column(name = "client_secret_hash", length = 255)
    private String clientSecretHash;

    @Column(name = "post_logout_redirect_uris")
    private String postLogoutRedirectUris;

    @Column(name = "require_consent", nullable = false)
    private boolean requireConsent;

    @Column(name = "status", nullable = false, length = 16)
    private OAuthClientStatus status = OAuthClientStatus.ACTIVE;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public UUID getAppId() {
        return appId;
    }

    public void setAppId(UUID appId) {
        this.appId = appId;
    }

    public OAuthClientType getClientType() {
        return clientType;
    }

    public void setClientType(OAuthClientType clientType) {
        this.clientType = clientType;
    }

    public String getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(String grantTypes) {
        this.grantTypes = grantTypes;
    }

    public String getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(String redirectUris) {
        this.redirectUris = redirectUris;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public boolean isPkceRequired() {
        return pkceRequired;
    }

    public void setPkceRequired(boolean pkceRequired) {
        this.pkceRequired = pkceRequired;
    }

    public String getTokenEndpointAuth() {
        return tokenEndpointAuth;
    }

    public void setTokenEndpointAuth(String tokenEndpointAuth) {
        this.tokenEndpointAuth = tokenEndpointAuth;
    }

    public String getClientSecretHash() {
        return clientSecretHash;
    }

    public void setClientSecretHash(String clientSecretHash) {
        this.clientSecretHash = clientSecretHash;
    }

    public String getPostLogoutRedirectUris() {
        return postLogoutRedirectUris;
    }

    public void setPostLogoutRedirectUris(String postLogoutRedirectUris) {
        this.postLogoutRedirectUris = postLogoutRedirectUris;
    }

    public boolean isRequireConsent() {
        return requireConsent;
    }

    public void setRequireConsent(boolean requireConsent) {
        this.requireConsent = requireConsent;
    }

    public OAuthClientStatus getStatus() {
        return status;
    }

    public void setStatus(OAuthClientStatus status) {
        this.status = status;
    }
}
