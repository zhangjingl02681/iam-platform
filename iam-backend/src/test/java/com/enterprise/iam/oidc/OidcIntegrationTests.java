package com.enterprise.iam.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterprise.iam.application.domain.IamOAuthClient;
import com.enterprise.iam.application.repo.IamOAuthClientRepository;
import com.enterprise.iam.identity.domain.IamUser;
import com.enterprise.iam.identity.repo.IamUserRepository;
import com.enterprise.iam.support.EmbeddedPostgresSupport;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OidcIntegrationTests extends EmbeddedPostgresSupport {

    private static final String ADMIN = "admin";
    private static final String ADMIN_PASSWORD = "test-Admin-Passw0rd!";
    private static final String WEB_CLIENT = "iam-demo-web";
    private static final String WEB_SECRET = "DevWeb_ClientSecret_Only!";
    private static final String API_CLIENT = "iam-demo-api";
    private static final String API_SECRET = "DevApi_ClientSecret_Only!";
    private static final String REDIRECT_URI = "http://localhost:8080/login/oauth2/code/iam-demo-web";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IamUserRepository userRepository;

    @Autowired
    private IamOAuthClientRepository oauthClientRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void openIdConfigurationExposesIssuerAndProtocolEndpoints() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("http://localhost:8080"))
                .andExpect(jsonPath("$.authorization_endpoint").value("http://localhost:8080/oauth2/authorize"))
                .andExpect(jsonPath("$.token_endpoint").value("http://localhost:8080/oauth2/token"))
                .andExpect(jsonPath("$.userinfo_endpoint").value("http://localhost:8080/userinfo"))
                .andExpect(jsonPath("$.jwks_uri").value("http://localhost:8080/oauth2/jwks"))
                .andExpect(jsonPath("$.revocation_endpoint").value("http://localhost:8080/oauth2/revoke"))
                .andExpect(jsonPath("$.introspection_endpoint").value("http://localhost:8080/oauth2/introspect"))
                .andExpect(jsonPath("$.end_session_endpoint").value("http://localhost:8080/connect/logout"));
    }

    @Test
    void authorizationCodePkceIssuesIdTokenWithUserUuidSubjectAndUserInfoWorks() throws Exception {
        IamUser admin = userRepository.findByUsername(ADMIN).orElseThrow();
        Pkce pkce = Pkce.generate();

        MvcResult authorize = mockMvc.perform(get("/oauth2/authorize")
                        .queryParam("response_type", "code")
                        .queryParam("client_id", WEB_CLIENT)
                        .queryParam("redirect_uri", REDIRECT_URI)
                        .queryParam("scope", "openid profile email")
                        .queryParam("state", "state-xyz")
                        .queryParam("code_challenge", pkce.challenge())
                        .queryParam("code_challenge_method", "S256")
                        .accept(MediaType.TEXT_HTML))
                .andReturn();

        MockHttpSession session = (MockHttpSession) authorize.getRequest().getSession(true);
        authorize = followRedirectsUntil(session, authorize, location -> location.contains("code="));

        String location = authorize.getResponse().getRedirectedUrl();
        assertThat(location).isNotBlank().startsWith(REDIRECT_URI);
        String code = queryParam(location, "code");
        assertThat(code).isNotBlank();
        assertThat(queryParam(location, "state")).isEqualTo("state-xyz");

        Map<String, Object> tokens = requestToken(WEB_CLIENT, WEB_SECRET, Map.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", REDIRECT_URI,
                "code_verifier", pkce.verifier()));

        String accessToken = (String) tokens.get("access_token");
        String idToken = (String) tokens.get("id_token");
        String refreshToken = (String) tokens.get("refresh_token");
        assertThat(accessToken).isNotBlank();
        assertThat(idToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        Map<String, Object> claims = decodeJwtClaims(idToken);
        assertThat(claims.get("sub")).isEqualTo(admin.getId().toString());
        assertThat(claims.get("iss")).isEqualTo("http://localhost:8080");
        assertThat(claims.get("preferred_username")).isEqualTo(ADMIN);
        assertThat(claims.get("name")).isEqualTo(admin.getDisplayName());

        mockMvc.perform(get("/userinfo").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value(admin.getId().toString()))
                .andExpect(jsonPath("$.preferred_username").value(ADMIN))
                .andExpect(jsonPath("$.name").value(admin.getDisplayName()));

        IamOAuthClient webClient = oauthClientRepository.findByClientId(WEB_CLIENT).orElseThrow();
        assertThat(webClient.getClientSecretHash()).isNotEqualTo(WEB_SECRET);
        assertThat(webClient.getClientSecretHash()).startsWith("$argon2id$");
    }

    @Test
    void clientCredentialsDemoApiClientObtainsAccessToken() throws Exception {
        Map<String, Object> tokens = requestToken(API_CLIENT, API_SECRET, Map.of(
                "grant_type", "client_credentials",
                "scope", "api"));

        assertThat((String) tokens.get("access_token")).isNotBlank();
        assertThat(tokens.get("token_type").toString()).isEqualToIgnoringCase("Bearer");
        assertThat(((Number) tokens.get("expires_in")).intValue()).isBetween(300, 15 * 60);

        IamOAuthClient apiClient = oauthClientRepository.findByClientId(API_CLIENT).orElseThrow();
        assertThat(apiClient.getClientSecretHash()).isNotEqualTo(API_SECRET);
        assertThat(apiClient.getClientSecretHash()).startsWith("$argon2id$");
    }

    @Test
    void refreshTokenIsRotatedAndReuseIsRejected() throws Exception {
        Pkce pkce = Pkce.generate();
        MvcResult authorize = mockMvc.perform(get("/oauth2/authorize")
                        .queryParam("response_type", "code")
                        .queryParam("client_id", WEB_CLIENT)
                        .queryParam("redirect_uri", REDIRECT_URI)
                        .queryParam("scope", "openid profile")
                        .queryParam("state", "refresh-state")
                        .queryParam("code_challenge", pkce.challenge())
                        .queryParam("code_challenge_method", "S256")
                        .accept(MediaType.TEXT_HTML))
                .andReturn();
        MockHttpSession session = (MockHttpSession) authorize.getRequest().getSession(true);
        authorize = followRedirectsUntil(session, authorize, location -> location.contains("code="));
        String code = queryParam(authorize.getResponse().getRedirectedUrl(), "code");

        Map<String, Object> initial = requestToken(WEB_CLIENT, WEB_SECRET, Map.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", REDIRECT_URI,
                "code_verifier", pkce.verifier()));
        String oldRefresh = (String) initial.get("refresh_token");
        assertThat(oldRefresh).isNotBlank();

        Map<String, Object> rotated = requestToken(WEB_CLIENT, WEB_SECRET, Map.of(
                "grant_type", "refresh_token",
                "refresh_token", oldRefresh));
        String newRefresh = (String) rotated.get("refresh_token");
        assertThat((String) rotated.get("access_token")).isNotBlank();
        assertThat(newRefresh).isNotBlank().isNotEqualTo(oldRefresh);

        mockMvc.perform(post("/oauth2/token")
                        .with(httpBasic(WEB_CLIENT, WEB_SECRET))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(form("grant_type", "refresh_token", "refresh_token", oldRefresh)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actuatorHealthStaysOpenAndManagementApiStillAcceptsBasic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value(ADMIN));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requestToken(String clientId, String secret, Map<String, String> params)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/oauth2/token")
                        .with(httpBasic(clientId, secret))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(form(params)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
    }

    private MvcResult followRedirectsUntil(
            MockHttpSession session, MvcResult current, java.util.function.Predicate<String> done)
            throws Exception {
        for (int i = 0; i < 8; i++) {
            int status = current.getResponse().getStatus();
            String location = current.getResponse().getRedirectedUrl();
            if (location != null && location.startsWith(REDIRECT_URI)) {
                if (location.contains("error=")) {
                    throw new AssertionError("Authorization failed: " + location);
                }
                return current;
            }
            if (location != null && done.test(location)) {
                return current;
            }
            if (status == 200 && location == null && isLoginPage(current)) {
                current = mockMvc.perform(post("/login")
                                .session(session)
                                .with(csrf())
                                .param("username", ADMIN)
                                .param("password", ADMIN_PASSWORD)
                                .accept(MediaType.TEXT_HTML))
                        .andReturn();
                continue;
            }
            if (location == null) {
                throw new AssertionError("Expected a redirect, got status " + status + " body="
                        + current.getResponse().getContentAsString());
            }
            current = mockMvc.perform(followLocation(location)
                            .session(session)
                            .accept(MediaType.TEXT_HTML))
                    .andReturn();
        }
        throw new AssertionError("Did not obtain an authorization code. Last location="
                + current.getResponse().getRedirectedUrl());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder followLocation(
            String location) {
        URI uri = URI.create(location);
        String path = uri.getPath() == null ? "/" : uri.getPath();
        var request = get(path);
        String rawQuery = uri.getRawQuery();
        if (rawQuery != null && !rawQuery.isBlank()) {
            for (String pair : rawQuery.split("&")) {
                if (pair.isEmpty()) {
                    continue;
                }
                int eq = pair.indexOf('=');
                String name;
                String value;
                if (eq < 0) {
                    name = URLDecoder.decode(pair, StandardCharsets.UTF_8);
                    value = "";
                } else {
                    name = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                    value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                }
                request.queryParam(name, value);
            }
        }
        return request;
    }

    private static boolean isLoginPage(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        String path = result.getRequest().getRequestURI();
        return "/login".equals(path) || (body != null && body.contains("name=\"username\""));
    }


    private static String queryParam(String location, String name) {
        return UriComponentsBuilder.fromUriString(location)
                .build(true)
                .getQueryParams()
                .getFirst(name);
    }

    private static LinkedMultiValueMap<String, String> form(Map<String, String> params) {
        LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        params.forEach(map::add);
        return map;
    }

    private static LinkedMultiValueMap<String, String> form(String... keyValues) {
        LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.add(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    private Map<String, Object> decodeJwtClaims(String jwt) throws Exception {
        String payload = jwt.split("\\.")[1];
        byte[] decoded = Base64.getUrlDecoder().decode(payload);
        @SuppressWarnings("unchecked")
        Map<String, Object> claims = objectMapper.readValue(decoded, Map.class);
        return claims;
    }

    private record Pkce(String verifier, String challenge) {
        static Pkce generate() throws Exception {
            byte[] bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            String verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
            return new Pkce(verifier, challenge);
        }
    }
}
