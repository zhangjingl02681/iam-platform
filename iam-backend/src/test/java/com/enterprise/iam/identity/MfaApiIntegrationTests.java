package com.enterprise.iam.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterprise.iam.audit.repo.IamAuditEventRepository;
import com.enterprise.iam.identity.domain.IamMfaFactor;
import com.enterprise.iam.identity.repo.IamMfaFactorRepository;
import com.enterprise.iam.identity.service.TotpService;
import com.enterprise.iam.support.EmbeddedPostgresSupport;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MfaApiIntegrationTests extends EmbeddedPostgresSupport {

    private static final String ADMIN = "admin";
    private static final String ADMIN_PASSWORD = "test-Admin-Passw0rd!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TotpService totpService;

    @Autowired
    private IamMfaFactorRepository factorRepository;

    @Autowired
    private IamAuditEventRepository auditEventRepository;

    @Test
    void unauthenticatedEnrollmentIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/me/mfa/totp/enrollments").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me/mfa/factors")).andExpect(status().isUnauthorized());
    }

    @Test
    void enrollAndConfirmStoresSecretOutsidePlaintextAndReturnsRecoveryCodesOnce() throws Exception {
        UserFixture user = createUser();

        MvcResult started = mockMvc.perform(post("/api/v1/me/mfa/totp/enrollments")
                        .with(httpBasic(user.username(), user.password()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label": "Authenticator"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.secret").isNotEmpty())
                .andExpect(jsonPath("$.otpauthUri").isNotEmpty())
                .andExpect(jsonPath("$.otpauthUri").value(org.hamcrest.Matchers.startsWith("otpauth://totp/")))
                .andExpect(jsonPath("$.verifiedAt").isEmpty())
                .andReturn();

        String body = started.getResponse().getContentAsString();
        String factorId = com.jayway.jsonpath.JsonPath.read(body, "$.id");
        String secret = com.jayway.jsonpath.JsonPath.read(body, "$.secret");

        mockMvc.perform(post("/api/v1/me/mfa/totp/enrollments/" + factorId + "/confirm")
                        .with(httpBasic(user.username(), user.password()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "000000"}
                                """))
                .andExpect(status().isBadRequest());

        String code = totpService.currentCode(secret);
        MvcResult confirmed = mockMvc.perform(post("/api/v1/me/mfa/totp/enrollments/" + factorId + "/confirm")
                        .with(httpBasic(user.username(), user.password()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "%s"}
                                """.formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.factor.id").value(factorId))
                .andExpect(jsonPath("$.factor.verifiedAt").isNotEmpty())
                .andExpect(jsonPath("$.factor.secret").doesNotExist())
                .andExpect(jsonPath("$.factor.secretEnc").doesNotExist())
                .andExpect(jsonPath("$.secret").doesNotExist())
                .andExpect(jsonPath("$.recoveryCodes").isArray())
                .andExpect(jsonPath("$.recoveryCodes.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(8)))
                .andReturn();

        IamMfaFactor stored = factorRepository.findById(UUID.fromString(factorId)).orElseThrow();
        assertThat(stored.getSecretEnc()).isNotBlank().isNotEqualTo(secret);
        assertThat(stored.getVerifiedAt()).isNotNull();
        assertThat(stored.getDisabledAt()).isNull();

        mockMvc.perform(get("/api/v1/me/mfa/factors").with(httpBasic(user.username(), user.password())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(factorId))
                .andExpect(jsonPath("$[0].type").value("totp"))
                .andExpect(jsonPath("$[0].verifiedAt").isNotEmpty())
                .andExpect(jsonPath("$[0].secret").doesNotExist())
                .andExpect(jsonPath("$[0].secretEnc").doesNotExist())
                .andExpect(jsonPath("$[0].otpauthUri").doesNotExist());

        assertThat(auditEventRepository.countByObjectIdAndEventType(UUID.fromString(factorId), "mfa.enroll"))
                .isGreaterThanOrEqualTo(1);
        assertThat(auditEventRepository.countByObjectIdAndEventType(UUID.fromString(factorId), "mfa.verify"))
                .isEqualTo(1);

        mockMvc.perform(get("/api/v1/users/" + user.id() + "/mfa/factors")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(factorId))
                .andExpect(jsonPath("$[0].secret").doesNotExist())
                .andExpect(jsonPath("$[0].secretEnc").doesNotExist())
                .andExpect(jsonPath("$[0].otpauthUri").doesNotExist());

        mockMvc.perform(get("/api/v1/users/" + user.id() + "/mfa/factors")
                        .with(httpBasic(user.username(), user.password())))
                .andExpect(status().isForbidden());

        List<String> recovery = com.jayway.jsonpath.JsonPath.read(
                confirmed.getResponse().getContentAsString(), "$.recoveryCodes");
        assertThat(recovery).isNotEmpty();
    }

    @Test
    void formLoginRequiresTotpAndSucceedsWithValidCode() throws Exception {
        UserFixture user = enrollVerifiedUser();

        MvcResult pending = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", user.username())
                        .param("password", user.password())
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertThat(pending.getResponse().getRedirectedUrl()).contains("mfa=required");
        MockHttpSession session = (MockHttpSession) pending.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/v1/me").session(session).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        MvcResult invalid = mockMvc.perform(post("/login/mfa")
                        .session(session)
                        .with(csrf())
                        .param("code", "000000")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertThat(invalid.getResponse().getRedirectedUrl()).contains("mfa_invalid");

        mockMvc.perform(get("/api/v1/me").session(session).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        String valid = totpService.currentCode(user.totpSecret());
        MvcResult completed = mockMvc.perform(post("/login/mfa")
                        .session(session)
                        .with(csrf())
                        .param("code", valid)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertThat(completed.getResponse().getRedirectedUrl()).doesNotContain("mfa=required");
        MockHttpSession authenticated = (MockHttpSession) completed.getRequest().getSession(false);

        mockMvc.perform(get("/api/v1/me").session(authenticated).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value(user.username()));
    }

    @Test
    void recoveryCodeWorksOnceAndReuseIsRejected() throws Exception {
        UserFixture user = enrollVerifiedUser();
        MockHttpSession first = startMfaPending(user);

        MvcResult used = mockMvc.perform(post("/login/mfa")
                        .session(first)
                        .with(csrf())
                        .param("code", user.recoveryCodes().getFirst())
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertThat(used.getResponse().getRedirectedUrl()).doesNotContain("error=");
        MockHttpSession authenticated = (MockHttpSession) used.getRequest().getSession(false);
        mockMvc.perform(get("/api/v1/me").session(authenticated).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        MockHttpSession second = startMfaPending(user);
        MvcResult reused = mockMvc.perform(post("/login/mfa")
                        .session(second)
                        .with(csrf())
                        .param("code", user.recoveryCodes().getFirst())
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertThat(reused.getResponse().getRedirectedUrl()).contains("mfa_invalid");
        mockMvc.perform(get("/api/v1/me").session(second).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disableFactorAllowsPasswordOnlyLoginAndAdminCanDisable() throws Exception {
        UserFixture user = enrollVerifiedUser();

        mockMvc.perform(post("/api/v1/me/mfa/factors/" + user.factorId() + "/disable")
                        .with(httpBasic(user.username(), user.password())))
                .andExpect(status().isNoContent());

        MockHttpSession session = formLogin(user.username(), user.password());
        mockMvc.perform(get("/api/v1/me").session(session).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value(user.username()));

        UserFixture other = enrollVerifiedUser();
        mockMvc.perform(post("/api/v1/users/" + other.id() + "/mfa/factors/" + other.factorId() + "/disable")
                        .with(httpBasic(user.username(), user.password())))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/users/" + other.id() + "/mfa/factors/" + other.factorId() + "/disable")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isNoContent());

        MockHttpSession otherSession = formLogin(other.username(), other.password());
        mockMvc.perform(get("/api/v1/me").session(otherSession).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    private MockHttpSession startMfaPending(UserFixture user) throws Exception {
        MvcResult pending = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", user.username())
                        .param("password", user.password())
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertThat(pending.getResponse().getRedirectedUrl()).contains("mfa=required");
        return (MockHttpSession) pending.getRequest().getSession(false);
    }

    private MockHttpSession formLogin(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", username)
                        .param("password", password)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertThat(result.getResponse().getRedirectedUrl()).doesNotContain("mfa=required");
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private UserFixture enrollVerifiedUser() throws Exception {
        UserFixture created = createUser();
        MvcResult started = mockMvc.perform(post("/api/v1/me/mfa/totp/enrollments")
                        .with(httpBasic(created.username(), created.password()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label": "Authenticator"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String secret = com.jayway.jsonpath.JsonPath.read(started.getResponse().getContentAsString(), "$.secret");
        String factorId = com.jayway.jsonpath.JsonPath.read(started.getResponse().getContentAsString(), "$.id");
        String code = totpService.currentCode(secret);
        MvcResult confirmed = mockMvc.perform(post("/api/v1/me/mfa/totp/enrollments/" + factorId + "/confirm")
                        .with(httpBasic(created.username(), created.password()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "%s"}
                                """.formatted(code)))
                .andExpect(status().isOk())
                .andReturn();
        List<String> recovery = com.jayway.jsonpath.JsonPath.read(
                confirmed.getResponse().getContentAsString(), "$.recoveryCodes");
        return new UserFixture(created.id(), created.username(), created.password(), secret, factorId, recovery);
    }

    private UserFixture createUser() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "mfa-" + suffix;
        String password = "Mfa-Passw0rd!";
        MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "displayName": "MFA User",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return new UserFixture(id, username, password, null, null, List.of());
    }

    private record UserFixture(
            String id, String username, String password, String totpSecret, String factorId, List<String> recoveryCodes) {}
}
