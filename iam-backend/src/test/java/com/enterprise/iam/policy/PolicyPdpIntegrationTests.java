package com.enterprise.iam.policy;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class PolicyPdpIntegrationTests extends EmbeddedPostgresSupport {

    private static final String ADMIN = "admin";
    private static final String ADMIN_PASSWORD = "test-Admin-Passw0rd!";
    private static final String USER_PASSWORD = "Regular-Passw0rd!";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pdpAllowsMatchingPolicyDeniesByDefaultAndIgnoresUnpublishedVersions() throws Exception {
        String suffix = idSuffix();
        String appCode = "pdp-app-" + suffix;
        String appId = createApplication(appCode, "策略应用");
        String financeUser = "fin-" + suffix;
        String otherUser = "other-" + suffix;
        createUser(financeUser, "财务用户");
        createUser(otherUser, "其他用户");

        String financeRoleId = createRole("finance-" + suffix, "财务");
        assignRole(financeUser, financeRoleId);

        MvcResult policy = mockMvc.perform(post("/api/v1/policies")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "allow-finance-%s", "name": "财务可启动", "description": "仅财务角色可启动"}
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("allow-finance-" + suffix))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        String policyId = com.jayway.jsonpath.JsonPath.read(policy.getResponse().getContentAsString(), "$.id");

        String allowDocument =
                """
                {
                  "effect": "allow",
                  "actions": ["app:launch"],
                  "resources": [{"type": "application", "code": "%s"}],
                  "when": {"roles": ["finance-%s"]}
                }
                """.formatted(appCode, suffix);

        MvcResult version = mockMvc.perform(post("/api/v1/policies/" + policyId + "/versions")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"document": %s, "comment": "初稿"}
                                """.formatted(toJsonString(allowDocument))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.checksum").isNotEmpty())
                .andReturn();
        String versionId = com.jayway.jsonpath.JsonPath.read(version.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/policies/" + policyId + "/versions").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(versionId));

        mockMvc.perform(post("/api/v1/pdp/decide")
                        .with(httpBasic(financeUser, USER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "app:launch", "resourceType": "application", "resourceId": "%s", "resourceCode": "%s"}
                                """.formatted(appId, appCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("deny"));

        mockMvc.perform(post("/api/v1/policies/" + policyId + "/versions/" + versionId + "/publish")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(post("/api/v1/policies/" + policyId + "/bindings")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"versionId": "%s", "targetType": "application", "targetId": "%s"}
                                """.formatted(versionId, appId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType").value("application"));

        mockMvc.perform(post("/api/v1/pdp/decide")
                        .with(httpBasic(financeUser, USER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "app:launch", "resourceType": "application", "resourceId": "%s", "resourceCode": "%s"}
                                """.formatted(appId, appCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("allow"))
                .andExpect(jsonPath("$.reason").value(org.hamcrest.Matchers.containsString(policyId)))
                .andExpect(jsonPath("$.policyId").value(policyId))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(post("/api/v1/pdp/decide")
                        .with(httpBasic(otherUser, USER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "app:launch", "resourceType": "application", "resourceId": "%s", "resourceCode": "%s"}
                                """.formatted(appId, appCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("deny"));

        mockMvc.perform(get("/api/v1/me/applications").with(httpBasic(financeUser, USER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[?(@.code == '%s')]".formatted(appCode)).isNotEmpty());
        mockMvc.perform(get("/api/v1/me/applications").with(httpBasic(otherUser, USER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[?(@.code == '%s')]".formatted(appCode)).isEmpty());
    }

    @Test
    void denyOverridesAllowAndUnpublishedPoliciesAreIgnoredForLaunch() throws Exception {
        String suffix = idSuffix();
        String appCode = "deny-app-" + suffix;
        String appId = createApplication(appCode, "拒绝覆盖应用");
        String user = "staff-" + suffix;
        createUser(user, "员工");

        String allowId = createPolicy("allow-all-" + suffix, "全员允许");
        String allowVersion = addVersion(
                allowId,
                """
                {"effect":"allow","actions":["app:launch"],"resources":[{"type":"application","id":"*"}]}
                """);
        publish(allowId, allowVersion);
        bind(allowId, allowVersion, appId);

        String draftDenyId = createPolicy("draft-deny-" + suffix, "未发布拒绝");
        String draftVersion = addVersion(
                draftDenyId,
                """
                {"effect":"deny","actions":["app:launch"],"resources":[{"type":"application","code":"%s"}]}
                """.formatted(appCode));
        // unpublished — must be ignored
        mockMvc.perform(post("/api/v1/policies/" + draftDenyId + "/bindings")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"versionId": "%s", "targetType": "application", "targetId": "%s"}
                                """.formatted(draftVersion, appId)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/me/applications").with(httpBasic(user, USER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[?(@.code == '%s')]".formatted(appCode)).isNotEmpty());

        String denyId = createPolicy("deny-all-" + suffix, "全员拒绝");
        String denyVersion = addVersion(
                denyId,
                """
                {"effect":"deny","actions":["app:launch"],"resources":[{"type":"application","code":"%s"}]}
                """.formatted(appCode));
        publish(denyId, denyVersion);
        bind(denyId, denyVersion, appId);

        mockMvc.perform(post("/api/v1/pdp/decide")
                        .with(httpBasic(user, USER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "app:launch", "resourceType": "application", "resourceId": "%s", "resourceCode": "%s"}
                                """.formatted(appId, appCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("deny"))
                .andExpect(jsonPath("$.reason").value(org.hamcrest.Matchers.containsString(denyId)));

        mockMvc.perform(get("/api/v1/me/applications").with(httpBasic(user, USER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[?(@.code == '%s')]".formatted(appCode)).isEmpty());
    }

    @Test
    void pdpDefaultsToDenyWhenNothingMatchesAndRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/pdp/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "user:read", "resourceType": "user"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/pdp/decide")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "user:read", "resourceType": "user"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("deny"))
                .andExpect(jsonPath("$.reason").value(org.hamcrest.Matchers.containsString("default")));

        String suffix = idSuffix();
        String user = "policyviewer-" + suffix;
        createUser(user, "普通用户");
        mockMvc.perform(post("/api/v1/policies")
                        .with(httpBasic(user, USER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "blocked-%s", "name": "不应创建"}
                                """.formatted(suffix)))
                .andExpect(status().isForbidden());
    }

    private String createApplication(String code, String name) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/applications")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "%s", "name": "%s", "launchUrl": "/examples/%s"}
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

    private String createRole(String code, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/roles")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "%s", "name": "%s"}
                                """.formatted(code, name)))
                .andExpect(status().isCreated())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private void assignRole(String username, String roleId) throws Exception {
        String userId = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/api/v1/me").with(httpBasic(username, USER_PASSWORD)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.profile.id");
        mockMvc.perform(post("/api/v1/users/" + userId + "/roles")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roleId": "%s"}
                                """.formatted(roleId)))
                .andExpect(status().isOk());
    }

    private String createPolicy(String code, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/policies")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "%s", "name": "%s"}
                                """.formatted(code, name)))
                .andExpect(status().isCreated())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String addVersion(String policyId, String document) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/policies/" + policyId + "/versions")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"document": %s}
                                """.formatted(toJsonString(document))))
                .andExpect(status().isCreated())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private void publish(String policyId, String versionId) throws Exception {
        mockMvc.perform(post("/api/v1/policies/" + policyId + "/versions/" + versionId + "/publish")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk());
    }

    private void bind(String policyId, String versionId, String applicationId) throws Exception {
        mockMvc.perform(post("/api/v1/policies/" + policyId + "/bindings")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"versionId": "%s", "targetType": "application", "targetId": "%s"}
                                """.formatted(versionId, applicationId)))
                .andExpect(status().isCreated());
    }

    private static String toJsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private static String idSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
