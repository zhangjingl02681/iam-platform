package com.enterprise.iam.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterprise.iam.application.domain.IamOAuthClient;
import com.enterprise.iam.application.repo.IamOAuthClientRepository;
import com.enterprise.iam.support.EmbeddedPostgresSupport;
import java.net.URI;
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
class OidcConsentIntegrationTests extends EmbeddedPostgresSupport {

    private static final String ADMIN = "admin";
    private static final String ADMIN_PASSWORD = "test-Admin-Passw0rd!";
    private static final String CONSENT_CLIENT = "iam-demo-consent";
    private static final String CONSENT_SECRET = "DevConsent_ClientSecret_Only!";
    private static final String REDIRECT_URI = "http://localhost:8080/login/oauth2/code/iam-demo-consent";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IamOAuthClientRepository oauthClientRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void consentClientRequiresUserConsentBeforeIssuingAuthorizationCode() throws Exception {
        IamOAuthClient client = oauthClientRepository.findByClientId(CONSENT_CLIENT).orElseThrow();
        assertThat(client.isRequireConsent()).isTrue();
        assertThat(client.getClientSecretHash()).isNotEqualTo(CONSENT_SECRET);
        assertThat(client.getClientSecretHash()).startsWith("$argon2id$");

        Pkce pkce = Pkce.generate();
        MvcResult authorize = mockMvc.perform(get("/oauth2/authorize")
                        .queryParam("response_type", "code")
                        .queryParam("client_id", CONSENT_CLIENT)
                        .queryParam("redirect_uri", REDIRECT_URI)
                        .queryParam("scope", "openid profile")
                        .queryParam("state", "consent-state")
                        .queryParam("code_challenge", pkce.challenge())
                        .queryParam("code_challenge_method", "S256")
                        .accept(MediaType.TEXT_HTML))
                .andReturn();
        MockHttpSession session = (MockHttpSession) authorize.getRequest().getSession(true);
        authorize = followUntil(session, authorize, location -> location != null && location.startsWith(REDIRECT_URI));

        String location = authorize.getResponse().getRedirectedUrl();
        assertThat(location).isNotBlank().startsWith(REDIRECT_URI);
        String code = queryParam(location, "code");
        assertThat(code).isNotBlank();
        assertThat(queryParam(location, "state")).isEqualTo("consent-state");

        MvcResult token = mockMvc.perform(post("/oauth2/token")
                        .with(httpBasic(CONSENT_CLIENT, CONSENT_SECRET))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(form(
                                "grant_type", "authorization_code",
                                "code", code,
                                "redirect_uri", REDIRECT_URI,
                                "code_verifier", pkce.verifier())))
                .andExpect(status().isOk())
                .andReturn();
        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(token.getResponse().getContentAsString(), Map.class);
        assertThat((String) body.get("access_token")).isNotBlank();
        assertThat((String) body.get("id_token")).isNotBlank();
    }

    private MvcResult followUntil(
            MockHttpSession session, MvcResult current, java.util.function.Predicate<String> done)
            throws Exception {
        boolean consented = false;
        for (int i = 0; i < 10; i++) {
            int status = current.getResponse().getStatus();
            String location = current.getResponse().getRedirectedUrl();
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
            if (status == 200 && location == null && isConsentPage(current) && !consented) {
                consented = true;
                var req = current.getRequest();
                String clientId = firstNonBlank(req.getParameter("client_id"), CONSENT_CLIENT);
                String state = firstNonBlank(req.getParameter("state"), null);
                String scope = firstNonBlank(req.getParameter("scope"), "openid profile");
                if (state == null) {
                    throw new AssertionError("Consent page is missing the authorization state parameter. query="
                            + req.getQueryString() + " params=" + req.getParameterMap().keySet());
                }
                var form = new LinkedMultiValueMap<String, String>();
                form.add("client_id", clientId);
                form.add("state", state);
                for (String item : scope.split("\\s+")) {
                    if (!item.isBlank()) {
                        form.add("scope", item);
                    }
                }
                current = mockMvc.perform(post("/oauth2/authorize")
                                .session(session)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .params(form)
                                .accept(MediaType.TEXT_HTML))
                        .andReturn();
                continue;
            }
            if (location == null) {
                throw new AssertionError("Expected redirect, login or consent, got status " + status
                        + " uri=" + current.getRequest().getRequestURI()
                        + " consented=" + consented
                        + " body=" + current.getResponse().getContentAsString());
            }
            current = mockMvc.perform(followLocation(location).session(session).accept(MediaType.TEXT_HTML))
                    .andReturn();
        }
        throw new AssertionError("Did not complete consent authorization. Last location="
                + current.getResponse().getRedirectedUrl());
    }

    private static boolean isLoginPage(MvcResult result) {
        String path = result.getRequest().getRequestURI();
        return "/login".equals(path);
    }

    private static boolean isConsentPage(MvcResult result) {
        String path = result.getRequest().getRequestURI();
        return "/consent".equals(path) || "/oauth2/consent".equals(path);
    }

    private static String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder followLocation(
            String location) {
        URI uri = URI.create(location);
        String path = uri.getPath() == null ? "/" : uri.getPath();
        var request = get(path);
        String rawQuery = uri.getRawQuery();
        if (rawQuery != null && !rawQuery.isBlank()) {
            for (String pair : rawQuery.split("&")) {
                int eq = pair.indexOf('=');
                if (eq < 0) {
                    request.queryParam(pair);
                } else {
                    String name = java.net.URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                    String value = java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                    request.queryParam(name, value);
                }
            }
        }
        return request;
    }

    private static String queryParam(String location, String name) {
        return UriComponentsBuilder.fromUriString(location).build(true).getQueryParams().getFirst(name);
    }

    private static LinkedMultiValueMap<String, String> form(String... keyValues) {
        LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.add(keyValues[i], keyValues[i + 1]);
        }
        return map;
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
