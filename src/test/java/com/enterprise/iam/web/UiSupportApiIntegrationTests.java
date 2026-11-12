package com.enterprise.iam.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterprise.iam.identity.domain.UserStatus;
import com.enterprise.iam.identity.repo.IamUserRepository;
import com.enterprise.iam.support.EmbeddedPostgresSupport;
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
class UiSupportApiIntegrationTests extends EmbeddedPostgresSupport {

    private static final String ADMIN = "admin";
    private static final String ADMIN_PASSWORD = "test-Admin-Passw0rd!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IamUserRepository userRepository;

    @Test
    void meIncludesAdminFlagAndAuthoritiesWithoutExposingSecrets() throws Exception {
        mockMvc.perform(get("/api/v1/me").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value(ADMIN))
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andExpect(jsonPath("$.admin").value(true))
                .andExpect(jsonPath("$.authorities").isArray())
                .andExpect(jsonPath("$.authorities[?(@ == 'ROLE_ADMIN')]").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.secretHash").doesNotExist())
                .andExpect(jsonPath("$.profile.password").doesNotExist());

        String suffix = idSuffix();
        createUser("user-" + suffix, "Regular User", "Regular-Passw0rd!");

        mockMvc.perform(get("/api/v1/me").with(httpBasic("user-" + suffix, "Regular-Passw0rd!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value("user-" + suffix))
                .andExpect(jsonPath("$.admin").value(false))
                .andExpect(jsonPath("$.authorities[?(@ == 'ROLE_ADMIN')]").doesNotExist());
    }

    @Test
    void browserFormLoginSessionIsEnoughToCallApiWithoutHttpBasic() throws Exception {
        MockHttpSession session = formLogin(ADMIN, ADMIN_PASSWORD);

        mockMvc.perform(get("/api/v1/me").session(session).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value(ADMIN))
                .andExpect(jsonPath("$.admin").value(true));
    }

    @Test
    void applicationsAdminCrudValidatesAndDoesNotExposeSecrets() throws Exception {
        String suffix = idSuffix();
        String code = "app-" + suffix;
        MvcResult created = mockMvc.perform(post("/api/v1/applications")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "%s",
                                  "name": "示例报销",
                                  "description": "费用报销系统",
                                  "launchUrl": "http://localhost:8080/examples/expense",
                                  "icon": "receipt",
                                  "accentColor": "#2457c5"
                                }
                                """.formatted(code)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.name").value("示例报销"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.launchUrl").value("http://localhost:8080/examples/expense"))
                .andExpect(jsonPath("$.icon").value("receipt"))
                .andExpect(jsonPath("$.accentColor").value("#2457c5"))
                .andExpect(jsonPath("$.secret").doesNotExist())
                .andExpect(jsonPath("$.clientSecretHash").doesNotExist())
                .andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/applications")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "%s", "name": "重复编码"}
                                """.formatted(code)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/applications")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "", "name": ""}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/v1/applications/" + id)
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "示例报销（已更新）", "status": "DISABLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("示例报销（已更新）"))
                .andExpect(jsonPath("$.status").value("DISABLED"));

        mockMvc.perform(get("/api/v1/applications/" + id).with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(get("/api/v1/applications")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .queryParam("q", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value(code));

        String user = "noadmin-" + suffix;
        createUser(user, "No Admin", "Regular-Passw0rd!");
        mockMvc.perform(post("/api/v1/applications")
                        .with(httpBasic(user, "Regular-Passw0rd!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "blocked-%s", "name": "不应创建"}
                                """.formatted(suffix)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/me/applications").with(httpBasic(user, "Regular-Passw0rd!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[?(@.code == '%s')]".formatted(code)).isEmpty())
                .andExpect(jsonPath("$.[?(@.code == 'iam-portal')].launchUrl").exists())
                .andExpect(jsonPath("$.[?(@.code == 'employee-handbook')].name").exists())
                .andExpect(jsonPath("$[0].secret").doesNotExist());
    }

    @Test
    void selfSessionsCanBeListedAndRevokedAndAdminCanSearch() throws Exception {
        MockHttpSession current = formLogin(ADMIN, ADMIN_PASSWORD);
        MockHttpSession other = formLogin(ADMIN, ADMIN_PASSWORD);

        MvcResult listed = mockMvc.perform(get("/api/v1/me/sessions").session(current))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.[?(@.current == true)]").isNotEmpty())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].secret").doesNotExist())
                .andReturn();

        String currentId = findSessionId(listed.getResponse().getContentAsString(), true);
        String otherId = findSessionId(listed.getResponse().getContentAsString(), false);
        assertThat(currentId).isNotBlank();
        assertThat(otherId).isNotBlank().isNotEqualTo(currentId);

        mockMvc.perform(post("/api/v1/me/sessions/revoke-others").session(current).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/me/sessions").session(current))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[?(@.id == '%s' && @.current == true && @.status == 'ACTIVE')]"
                        .formatted(currentId))
                        .isNotEmpty())
                .andExpect(jsonPath("$.[?(@.id == '%s' && @.status == 'REVOKED')]".formatted(otherId))
                        .isNotEmpty());

        mockMvc.perform(get("/api/v1/me").session(other).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        String suffix = idSuffix();
        String regular = "sess-" + suffix;
        createUser(regular, "Session User", "Regular-Passw0rd!");
        MockHttpSession regularSession = formLogin(regular, "Regular-Passw0rd!");
        MvcResult regularList = mockMvc.perform(get("/api/v1/me/sessions").session(regularSession))
                .andExpect(status().isOk())
                .andReturn();
        String regularSessionId =
                com.jayway.jsonpath.JsonPath.read(regularList.getResponse().getContentAsString(), "$[0].id");

        mockMvc.perform(delete("/api/v1/me/sessions/" + regularSessionId).session(current).with(csrf()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/sessions")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .queryParam("username", regular))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").exists())
                .andExpect(jsonPath("$.content[0].username").value(regular));

        mockMvc.perform(post("/api/v1/sessions/" + regularSessionId + "/revoke")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/me").session(regularSession).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/sessions").with(httpBasic(regular, "Regular-Passw0rd!")))
                .andExpect(status().isForbidden());
    }

    @Test
    void currentSessionCanBeRevokedBySelf() throws Exception {
        MockHttpSession session = formLogin(ADMIN, ADMIN_PASSWORD);
        MvcResult listed = mockMvc.perform(get("/api/v1/me/sessions").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String currentId = findSessionId(listed.getResponse().getContentAsString(), true);

        mockMvc.perform(delete("/api/v1/me/sessions/" + currentId).session(session).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/me").session(session).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanFilterAuditEventsAndMutationsAreRejected() throws Exception {
        String suffix = idSuffix();
        MvcResult created = mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "audit-%s",
                                  "displayName": "Audit User",
                                  "password": "Regular-Passw0rd!"
                                }
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andReturn();
        String userId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/audit-events")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .queryParam("action", "create")
                        .queryParam("objectId", userId)
                        .queryParam("eventType", "user.create"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].eventType").value("user.create"))
                .andExpect(jsonPath("$.content[0].action").value("create"))
                .andExpect(jsonPath("$.content[0].objectId").value(userId))
                .andExpect(jsonPath("$.content[0].eventHash").exists())
                .andExpect(jsonPath("$.content[0].secret").doesNotExist())
                .andExpect(jsonPath("$.content[0].secretHash").doesNotExist());

        String eventId = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/api/v1/audit-events")
                                .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                                .queryParam("objectId", userId))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.content[0].id");

        mockMvc.perform(delete("/api/v1/audit-events/" + eventId).with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(patch("/api/v1/audit-events/" + eventId)
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"result": "tampered"}
                                """))
                .andExpect(status().isMethodNotAllowed());

        String regular = "audit-reader-" + suffix;
        createUser(regular, "Audit Reader", "Regular-Passw0rd!");
        mockMvc.perform(get("/api/v1/audit-events").with(httpBasic(regular, "Regular-Passw0rd!")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/audit-events")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginReportsSafeErrorsAndRequiresCsrf() throws Exception {
        mockMvc.perform(get("/login").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/html")));
        mockMvc.perform(get("/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.parameterName").value("_csrf"));

        mockMvc.perform(post("/login")
                        .param("username", ADMIN)
                        .param("password", "wrong-password")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isForbidden());

        MvcResult bad = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "definitely-missing-user")
                        .param("password", "Whatever-Passw0rd!")
                        .accept(MediaType.TEXT_HTML))
                .andReturn();
        assertThat(redirectError(bad)).isEqualTo("bad_credentials");

        String suffix = idSuffix();
        String disabled = "disabled-" + suffix;
        String createdId = createUser(disabled, "Disabled User", "Regular-Passw0rd!");
        mockMvc.perform(patch("/api/v1/users/" + createdId)
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "DISABLED"}
                                """))
                .andExpect(status().isOk());
        MvcResult disabledLogin = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", disabled)
                        .param("password", "Regular-Passw0rd!")
                        .accept(MediaType.TEXT_HTML))
                .andReturn();
        assertThat(redirectError(disabledLogin)).isEqualTo("disabled");

        String locked = "locked-" + suffix;
        createUser(locked, "Locked User", "Regular-Passw0rd!");
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/login")
                            .with(csrf())
                            .param("username", locked)
                            .param("password", "Wrong-Passw0rd!")
                            .accept(MediaType.TEXT_HTML))
                    .andReturn();
        }
        MvcResult lockedLogin = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", locked)
                        .param("password", "Regular-Passw0rd!")
                        .accept(MediaType.TEXT_HTML))
                .andReturn();
        assertThat(redirectError(lockedLogin)).isEqualTo("locked");
    }

    @Test
    void anonymousHtmlFrontendRoutesRedirectToLoginWithoutSwallowingApis() throws Exception {
        mockMvc.perform(get("/admin").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/portal").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/applications"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorization_endpoint").exists());

        MockHttpSession session = formLogin(ADMIN, ADMIN_PASSWORD);
        mockMvc.perform(get("/admin/users").session(session).accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }

    private String createUser(String username, String displayName, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "displayName": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, displayName, password)))
                .andExpect(status().isCreated())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private MockHttpSession formLogin(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", username)
                        .param("password", password)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private static String redirectError(MvcResult result) {
        String location = result.getResponse().getRedirectedUrl();
        assertThat(location).as("expected a redirect to the login error page").isNotBlank();
        int idx = location.indexOf("error=");
        assertThat(idx).as("redirect should include error= : " + location).isGreaterThanOrEqualTo(0);
        String value = location.substring(idx + "error=".length());
        int amp = value.indexOf('&');
        return amp >= 0 ? value.substring(0, amp) : value;
    }

    private static String findSessionId(String json, boolean current) {
        net.minidev.json.JSONArray ids = com.jayway.jsonpath.JsonPath.read(
                json, "$.[?(@.current == " + current + " && @.status == 'ACTIVE')].id");
        assertThat(ids).isNotEmpty();
        return String.valueOf(ids.get(0));
    }

    private static String idSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
