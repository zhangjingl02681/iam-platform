package com.enterprise.iam.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterprise.iam.support.EmbeddedPostgresSupport;
import com.nimbusds.jwt.SignedJWT;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SigningKeyRotationIntegrationTests extends EmbeddedPostgresSupport {

    private static final String ADMIN = "admin";
    private static final String ADMIN_PASSWORD = "test-Admin-Passw0rd!";
    private static final String API_CLIENT = "iam-demo-api";
    private static final String API_SECRET = "DevApi_ClientSecret_Only!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rotateAddsNewKidToJwksSignsNewTokensAndRetainsPreviousUntilRetire() throws Exception {
        Map<String, Object> before = requestClientCredentials();
        String oldToken = (String) before.get("access_token");
        String oldKid = SignedJWT.parse(oldToken).getHeader().getKeyID();
        assertThat(oldKid).isNotBlank();
        assertThat(jwtDecoder.decode(oldToken).getTokenValue()).isEqualTo(oldToken);

        mockMvc.perform(get("/api/v1/signing-keys")).andExpect(status().isUnauthorized());

        String regular = "key-user-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "displayName": "Key User",
                                  "password": "Regular-Passw0rd!"
                                }
                                """.formatted(regular)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/signing-keys/rotate").with(httpBasic(regular, "Regular-Passw0rd!")))
                .andExpect(status().isForbidden());

        MvcResult rotated = mockMvc.perform(post("/api/v1/signing-keys/rotate").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current.kid").isNotEmpty())
                .andExpect(jsonPath("$.current.status").value("CURRENT"))
                .andExpect(jsonPath("$.previous.kid").value(oldKid))
                .andExpect(jsonPath("$.previous.status").value("PREVIOUS"))
                .andExpect(jsonPath("$.current.privateKey").doesNotExist())
                .andExpect(jsonPath("$.current.n").doesNotExist())
                .andExpect(jsonPath("$.current.d").doesNotExist())
                .andExpect(jsonPath("$.current.kty").doesNotExist())
                .andExpect(jsonPath("$.current.vaultRef").doesNotExist())
                .andReturn();
        String newKid = com.jayway.jsonpath.JsonPath.read(rotated.getResponse().getContentAsString(), "$.current.kid");
        assertThat(newKid).isNotEqualTo(oldKid);

        mockMvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[?(@.kid == '%s')]".formatted(oldKid)).isNotEmpty())
                .andExpect(jsonPath("$.keys[?(@.kid == '%s')]".formatted(newKid)).isNotEmpty())
                .andExpect(jsonPath("$.keys[0].d").doesNotExist())
                .andExpect(jsonPath("$.keys[0].p").doesNotExist())
                .andExpect(jsonPath("$.keys[0].q").doesNotExist());

        assertThat(jwtDecoder.decode(oldToken).getHeaders().get("kid")).isEqualTo(oldKid);

        Map<String, Object> after = requestClientCredentials();
        String newToken = (String) after.get("access_token");
        assertThat(SignedJWT.parse(newToken).getHeader().getKeyID()).isEqualTo(newKid);
        assertThat(jwtDecoder.decode(newToken).getHeaders().get("kid")).isEqualTo(newKid);

        mockMvc.perform(get("/api/v1/signing-keys").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.kid == '%s')].status".formatted(newKid)).value("CURRENT"))
                .andExpect(jsonPath("$[?(@.kid == '%s')].status".formatted(oldKid)).value("PREVIOUS"))
                .andExpect(jsonPath("$[0].d").doesNotExist())
                .andExpect(jsonPath("$[0].privateKey").doesNotExist())
                .andExpect(jsonPath("$[0].vaultRef").doesNotExist());

        Long privateInDb = jdbcTemplate.queryForObject(
                "select count(*) from iam_signing_key where vault_ref ilike '%BEGIN%' or kid = '-----BEGIN'",
                Long.class);
        assertThat(privateInDb).isZero();
        String vaultRef = jdbcTemplate.queryForObject(
                "select vault_ref from iam_signing_key where kid = ?", String.class, newKid);
        assertThat(vaultRef).isNotBlank();
        assertThat(vaultRef.toLowerCase()).doesNotContain("begin rsa").doesNotContain("\"d\":");

        mockMvc.perform(post("/api/v1/signing-keys/" + oldKid + "/retire").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kid").value(oldKid))
                .andExpect(jsonPath("$.status").value("RETIRED"))
                .andExpect(jsonPath("$.d").doesNotExist());

        mockMvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[?(@.kid == '%s')]".formatted(oldKid)).isEmpty())
                .andExpect(jsonPath("$.keys[?(@.kid == '%s')]".formatted(newKid)).isNotEmpty());

        assertThatThrownBy(() -> jwtDecoder.decode(oldToken)).isInstanceOf(JwtException.class);
        assertThat(jwtDecoder.decode(newToken).getHeaders().get("kid")).isEqualTo(newKid);
    }

    private Map<String, Object> requestClientCredentials() throws Exception {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("scope", "api");
        MvcResult result = mockMvc.perform(post("/oauth2/token")
                        .with(httpBasic(API_CLIENT, API_SECRET))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(form))
                .andExpect(status().isOk())
                .andReturn();
        @SuppressWarnings("unchecked")
        Map<String, Object> tokens = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return tokens;
    }
}
