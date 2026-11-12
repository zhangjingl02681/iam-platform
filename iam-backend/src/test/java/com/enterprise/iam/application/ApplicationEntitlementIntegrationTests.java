package com.enterprise.iam.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterprise.iam.audit.repo.IamAuditEventRepository;
import com.enterprise.iam.support.EmbeddedPostgresSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationEntitlementIntegrationTests extends EmbeddedPostgresSupport {

    private static final String ADMIN = "admin";
    private static final String ADMIN_PASSWORD = "test-Admin-Passw0rd!";
    private static final String USER_PASSWORD = "Regular-Passw0rd!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IamAuditEventRepository auditEventRepository;

    @Test
    void openAppStaysVisibleAndRestrictedAppsHonorUserOrgAndRoleGrants() throws Exception {
        String suffix = idSuffix();
        String openCode = "open-" + suffix;
        String restrictedCode = "restricted-" + suffix;

        String openId = createApplication(openCode, "开放应用-" + suffix);
        String restrictedId = createApplication(restrictedCode, "受限应用-" + suffix);

        String alice = "alice-" + suffix;
        String bob = "bob-" + suffix;
        String cara = "cara-" + suffix;
        String dave = "dave-" + suffix;
        String aliceId = createUser(alice, "Alice");
        String bobId = createUser(bob, "Bob");
        String caraId = createUser(cara, "Cara");
        createUser(dave, "Dave");

        mockMvc.perform(get("/api/v1/me/applications").with(httpBasic(dave, USER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[?(@.code == '%s')]".formatted(openCode)).isNotEmpty())
                .andExpect(jsonPath("$.[?(@.code == '%s')]".formatted(restrictedCode)).isNotEmpty())
                .andExpect(jsonPath("$.[?(@.code == 'iam-portal')]").isNotEmpty());

        MvcResult grant = mockMvc.perform(post("/api/v1/applications/" + restrictedId + "/entitlements")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectType": "USER", "subjectId": "%s"}
                                """.formatted(aliceId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subjectType").value("USER"))
                .andExpect(jsonPath("$.subjectId").value(aliceId))
                .andExpect(jsonPath("$.applicationId").value(restrictedId))
                .andReturn();
        String entitlementId = com.jayway.jsonpath.JsonPath.read(grant.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/applications/" + restrictedId + "/entitlements")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(entitlementId))
                .andExpect(jsonPath("$[0].subjectType").value("USER"));

        assertThat(auditEventRepository.countByObjectIdAndEventType(
                        UUID.fromString(restrictedId), "application.entitlement.add"))
                .isEqualTo(1);

        mockMvc.perform(get("/api/v1/me/applications").with(httpBasic(alice, USER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[?(@.code == '%s')]".formatted(openCode)).isNotEmpty())
                .andExpect(jsonPath("$.[?(@.code == '%s')]".formatted(restrictedCode)).isNotEmpty());

        mockMvc.perform(get("/api/v1/me/applications").with(httpBasic(dave, USER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[?(@.code == '%s')]".formatted(openCode)).isNotEmpty())
                .andExpect(jsonPath("$.[?(@.code == '%s')]".formatted(restrictedCode)).isEmpty())
                .andExpect(jsonPath("$.[?(@.code == 'iam-portal')]").isNotEmpty());

        MvcResult orgResult = mockMvc.perform(post("/api/v1/orgs")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "eng-%s", "name": "工程", "sortOrder": 1}
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andReturn();
        String orgId = com.jayway.jsonpath.JsonPath.read(orgResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/users/" + bobId + "/assignments")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orgId": "%s", "primary": true}
                                """.formatted(orgId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/applications/" + restrictedId + "/entitlements")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectType": "ORG", "subjectId": "%s"}
                                """.formatted(orgId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/me/applications").with(httpBasic(bob, USER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[?(@.code == '%s')]".formatted(restrictedCode)).isNotEmpty());

        MvcResult roleResult = mockMvc.perform(post("/api/v1/roles")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "finance-%s", "name": "财务"}
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andReturn();
        String roleId = com.jayway.jsonpath.JsonPath.read(roleResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/users/" + caraId + "/roles")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roleId": "%s"}
                                """.formatted(roleId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/applications/" + restrictedId + "/entitlements")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectType": "ROLE", "subjectId": "%s"}
                                """.formatted(roleId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/me/applications").with(httpBasic(cara, USER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[?(@.code == '%s')]".formatted(restrictedCode)).isNotEmpty());

        mockMvc.perform(get("/api/v1/me/applications").with(httpBasic(dave, USER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[?(@.code == '%s')]".formatted(restrictedCode)).isEmpty());

        mockMvc.perform(delete("/api/v1/applications/" + restrictedId + "/entitlements/" + entitlementId)
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isNoContent());
        assertThat(auditEventRepository.countByObjectIdAndEventType(
                        UUID.fromString(restrictedId), "application.entitlement.remove"))
                .isEqualTo(1);

        mockMvc.perform(get("/api/v1/applications/" + openId + "/entitlements")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void entitlementApisRejectAnonymousAndNonAdminUsers() throws Exception {
        String suffix = idSuffix();
        String appId = createApplication("authz-" + suffix, "授权校验应用");
        String user = "viewer-" + suffix;
        String userId = createUser(user, "Viewer");

        mockMvc.perform(get("/api/v1/applications/" + appId + "/entitlements"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/applications/" + appId + "/entitlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectType": "USER", "subjectId": "%s"}
                                """.formatted(userId)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/applications/" + appId + "/entitlements")
                        .with(httpBasic(user, USER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectType": "USER", "subjectId": "%s"}
                                """.formatted(userId)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/applications/" + appId + "/entitlements")
                        .with(httpBasic(user, USER_PASSWORD)))
                .andExpect(status().isForbidden());
    }

    private String createApplication(String code, String name) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/applications")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "%s",
                                  "name": "%s",
                                  "launchUrl": "/examples/%s"
                                }
                                """.formatted(code, name, code)))
                .andExpect(status().isCreated())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    }

    private String createUser(String username, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "displayName": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, displayName, USER_PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private static String idSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
