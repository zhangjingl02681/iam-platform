package com.enterprise.iam.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iam")
public class IamProperties {

    private final Security security = new Security();
    private final Bootstrap bootstrap = new Bootstrap();
    private final Oidc oidc = new Oidc();
    private final Vault vault = new Vault();
    private final Mfa mfa = new Mfa();
    private final Pam pam = new Pam();

    public Security getSecurity() {
        return security;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public Oidc getOidc() {
        return oidc;
    }

    public Vault getVault() {
        return vault;
    }

    public Mfa getMfa() {
        return mfa;
    }

    public Pam getPam() {
        return pam;
    }

    public static class Security {
        private int maxFailedAttempts = 5;
        private Duration lockDuration = Duration.ofMinutes(15);
        private int passwordHistorySize = 5;

        public int getMaxFailedAttempts() {
            return maxFailedAttempts;
        }

        public void setMaxFailedAttempts(int maxFailedAttempts) {
            this.maxFailedAttempts = maxFailedAttempts;
        }

        public Duration getLockDuration() {
            return lockDuration;
        }

        public void setLockDuration(Duration lockDuration) {
            this.lockDuration = lockDuration;
        }

        public int getPasswordHistorySize() {
            return passwordHistorySize;
        }

        public void setPasswordHistorySize(int passwordHistorySize) {
            this.passwordHistorySize = passwordHistorySize;
        }
    }

    public static class Bootstrap {
        private final Admin admin = new Admin();
        private final Clients clients = new Clients();

        public Admin getAdmin() {
            return admin;
        }

        public Clients getClients() {
            return clients;
        }
    }

    public static class Clients {
        private boolean enabled = false;
        private String webSecret = "";
        private String apiSecret = "";
        private String consentSecret = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getWebSecret() {
            return webSecret;
        }

        public void setWebSecret(String webSecret) {
            this.webSecret = webSecret;
        }

        public String getApiSecret() {
            return apiSecret;
        }

        public void setApiSecret(String apiSecret) {
            this.apiSecret = apiSecret;
        }

        public String getConsentSecret() {
            return consentSecret;
        }

        public void setConsentSecret(String consentSecret) {
            this.consentSecret = consentSecret;
        }
    }

    public static class Oidc {
        private String issuer = "http://localhost:8080";
        private String jwkLocation = ".data/dev-signing-jwk.json";
        private Duration accessTokenTtl = Duration.ofMinutes(10);
        private Duration refreshTokenTtl = Duration.ofHours(8);
        private Duration authorizationCodeTtl = Duration.ofMinutes(5);
        private Duration idTokenTtl = Duration.ofMinutes(15);
        private Duration sessionIdleTimeout = Duration.ofMinutes(30);
        private Duration sessionAbsoluteTimeout = Duration.ofHours(8);
        private Duration signingKeyGrace = Duration.ofDays(7);

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getJwkLocation() {
            return jwkLocation;
        }

        public void setJwkLocation(String jwkLocation) {
            this.jwkLocation = jwkLocation;
        }

        public Duration getAccessTokenTtl() {
            return accessTokenTtl;
        }

        public void setAccessTokenTtl(Duration accessTokenTtl) {
            this.accessTokenTtl = accessTokenTtl;
        }

        public Duration getRefreshTokenTtl() {
            return refreshTokenTtl;
        }

        public void setRefreshTokenTtl(Duration refreshTokenTtl) {
            this.refreshTokenTtl = refreshTokenTtl;
        }

        public Duration getAuthorizationCodeTtl() {
            return authorizationCodeTtl;
        }

        public void setAuthorizationCodeTtl(Duration authorizationCodeTtl) {
            this.authorizationCodeTtl = authorizationCodeTtl;
        }

        public Duration getIdTokenTtl() {
            return idTokenTtl;
        }

        public void setIdTokenTtl(Duration idTokenTtl) {
            this.idTokenTtl = idTokenTtl;
        }

        public Duration getSessionIdleTimeout() {
            return sessionIdleTimeout;
        }

        public void setSessionIdleTimeout(Duration sessionIdleTimeout) {
            this.sessionIdleTimeout = sessionIdleTimeout;
        }

        public Duration getSessionAbsoluteTimeout() {
            return sessionAbsoluteTimeout;
        }

        public void setSessionAbsoluteTimeout(Duration sessionAbsoluteTimeout) {
            this.sessionAbsoluteTimeout = sessionAbsoluteTimeout;
        }

        public Duration getSigningKeyGrace() {
            return signingKeyGrace;
        }

        public void setSigningKeyGrace(Duration signingKeyGrace) {
            this.signingKeyGrace = signingKeyGrace;
        }
    }

    public static class Vault {
        /**
         * {@code file} uses a gitignored local directory (development). {@code reserved}
         * is the production interface placeholder and does not call a cloud KMS.
         */
        private String type = "file";
        private String directory = ".data/vault";

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {
            this.directory = directory;
        }
    }

    public static class Mfa {
        private String issuer = "Enterprise IAM";
        private Duration pendingTtl = Duration.ofMinutes(5);
        private int recoveryCodeCount = 10;

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public Duration getPendingTtl() {
            return pendingTtl;
        }

        public void setPendingTtl(Duration pendingTtl) {
            this.pendingTtl = pendingTtl;
        }

        public int getRecoveryCodeCount() {
            return recoveryCodeCount;
        }

        public void setRecoveryCodeCount(int recoveryCodeCount) {
            this.recoveryCodeCount = recoveryCodeCount;
        }
    }

    public static class Pam {
        private Duration checkoutTtl = Duration.ofMinutes(30);

        public Duration getCheckoutTtl() {
            return checkoutTtl;
        }

        public void setCheckoutTtl(Duration checkoutTtl) {
            this.checkoutTtl = checkoutTtl;
        }
    }

    public static class Admin {
        private boolean enabled = false;
        private String username = "admin";
        private String password = "";
        private String displayName = "系统管理员";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
}
