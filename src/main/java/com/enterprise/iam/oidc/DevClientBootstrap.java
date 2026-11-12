package com.enterprise.iam.oidc;

import com.enterprise.iam.application.domain.ApplicationStatus;
import com.enterprise.iam.application.domain.IamApplication;
import com.enterprise.iam.application.domain.IamOAuthClient;
import com.enterprise.iam.application.domain.IamScope;
import com.enterprise.iam.application.domain.OAuthClientStatus;
import com.enterprise.iam.application.domain.OAuthClientType;
import com.enterprise.iam.application.repo.IamApplicationRepository;
import com.enterprise.iam.application.repo.IamOAuthClientRepository;
import com.enterprise.iam.application.repo.IamScopeRepository;
import com.enterprise.iam.config.IamProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@Order(2)
public class DevClientBootstrap implements ApplicationRunner {

    public static final String WEB_CLIENT_ID = "iam-demo-web";
    public static final String API_CLIENT_ID = "iam-demo-api";
    public static final String CONSENT_CLIENT_ID = "iam-demo-consent";
    public static final String WEB_REDIRECT_URI = "http://localhost:8080/login/oauth2/code/iam-demo-web";
    public static final String WEB_LOOPBACK_REDIRECT_URI = "http://127.0.0.1:8080/login/oauth2/code/iam-demo-web";
    public static final String CONSENT_REDIRECT_URI = "http://localhost:8080/login/oauth2/code/iam-demo-consent";
    public static final String CONSENT_LOOPBACK_REDIRECT_URI = "http://127.0.0.1:8080/login/oauth2/code/iam-demo-consent";

    private static final Logger log = LoggerFactory.getLogger(DevClientBootstrap.class);

    private final IamProperties properties;
    private final IamApplicationRepository applicationRepository;
    private final IamOAuthClientRepository clientRepository;
    private final IamScopeRepository scopeRepository;
    private final PasswordEncoder passwordEncoder;

    public DevClientBootstrap(
            IamProperties properties,
            IamApplicationRepository applicationRepository,
            IamOAuthClientRepository clientRepository,
            IamScopeRepository scopeRepository,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.applicationRepository = applicationRepository;
        this.clientRepository = clientRepository;
        this.scopeRepository = scopeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        IamProperties.Clients clients = properties.getBootstrap().getClients();
        if (!clients.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(clients.getWebSecret()) || !StringUtils.hasText(clients.getApiSecret())) {
            log.warn("OIDC demo clients are enabled but secrets are missing; skipping");
            return;
        }
        seedScope("openid", "OpenID Connect");
        seedScope("profile", "Profile claims");
        seedScope("email", "Email claim");
        seedScope("api", "Machine API access");
        seedCatalogApp(
                "iam-portal",
                "身份门户",
                "企业内部身份门户，查看可访问的应用与个人资料。",
                "http://localhost:8080/",
                "grid",
                "#1f3a5f");
        seedCatalogApp(
                "employee-handbook",
                "员工手册",
                "查阅制度、入职指引与常用政策（示例应用）。",
                "http://localhost:8080/examples/handbook",
                "book",
                "#3b5b7c");
        seedCatalogApp(
                "hr-directory",
                "员工通讯录",
                "查找同事与组织联系方式（示例应用）。",
                "http://localhost:8080/examples/directory",
                "users",
                "#2a6f6a");
        IamApplication application = applicationRepository.findByCode("iam-demo").orElseGet(() -> {
            IamApplication created = new IamApplication();
            created.setCode("iam-demo");
            created.setName("IAM Dev Clients");
            created.setDescription("Development-only OIDC / OAuth clients");
            created.setStatus(ApplicationStatus.ACTIVE);
            return applicationRepository.save(created);
        });
        seedClient(
                application,
                WEB_CLIENT_ID,
                "authorization_code refresh_token",
                WEB_REDIRECT_URI + " " + WEB_LOOPBACK_REDIRECT_URI,
                "openid profile email",
                true,
                false,
                "client_secret_basic",
                clients.getWebSecret(),
                "http://localhost:8080/");
        seedClient(
                application,
                API_CLIENT_ID,
                "client_credentials",
                null,
                "api",
                false,
                false,
                "client_secret_basic",
                clients.getApiSecret(),
                null);
        if (StringUtils.hasText(clients.getConsentSecret())) {
            seedClient(
                    application,
                    CONSENT_CLIENT_ID,
                    "authorization_code refresh_token",
                    CONSENT_REDIRECT_URI + " " + CONSENT_LOOPBACK_REDIRECT_URI,
                    "openid profile email",
                    true,
                    true,
                    "client_secret_basic",
                    clients.getConsentSecret(),
                    "http://localhost:8080/");
        }
        log.info("Ensured development OIDC clients '{}', '{}' and '{}'. Secrets are documented in the README only.",
                WEB_CLIENT_ID, API_CLIENT_ID, CONSENT_CLIENT_ID);
    }

    private void seedScope(String code, String description) {
        if (scopeRepository.existsById(code)) {
            return;
        }
        IamScope scope = new IamScope();
        scope.setCode(code);
        scope.setDescription(description);
        scopeRepository.save(scope);
    }

    private void seedClient(
            IamApplication application,
            String clientId,
            String grantTypes,
            String redirectUris,
            String scopes,
            boolean pkceRequired,
            boolean requireConsent,
            String tokenEndpointAuth,
            String rawSecret,
            String postLogoutRedirectUris) {
        IamOAuthClient client = clientRepository.findByClientId(clientId).orElse(null);
        if (client != null) {
            log.info("OAuth client '{}' already exists; secret was left unchanged.", clientId);
            return;
        }
        client = new IamOAuthClient();
        client.setAppId(application.getId());
        client.setClientId(clientId);
        client.setClientType(OAuthClientType.CONFIDENTIAL);
        client.setGrantTypes(grantTypes);
        client.setRedirectUris(redirectUris);
        client.setPostLogoutRedirectUris(postLogoutRedirectUris);
        client.setScopes(scopes);
        client.setPkceRequired(pkceRequired);
        client.setRequireConsent(requireConsent);
        client.setTokenEndpointAuth(tokenEndpointAuth);
        client.setClientSecretHash(passwordEncoder.encode(rawSecret));
        client.setStatus(OAuthClientStatus.ACTIVE);
        clientRepository.save(client);
    }

    private void seedCatalogApp(
            String code, String name, String description, String launchUrl, String icon, String accentColor) {
        if (applicationRepository.findByCode(code).isPresent()) {
            return;
        }
        IamApplication application = new IamApplication();
        application.setCode(code);
        application.setName(name);
        application.setDescription(description);
        application.setLaunchUrl(launchUrl);
        application.setIcon(icon);
        application.setAccentColor(accentColor);
        application.setStatus(ApplicationStatus.ACTIVE);
        applicationRepository.save(application);
    }
}
