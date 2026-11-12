package com.enterprise.iam.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterprise.iam.audit.repo.IamAuditEventRepository;
import com.enterprise.iam.identity.repo.IamRoleRepository;
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
class RoleApiIntegrationTests extends EmbeddedPostgresSupport {

    private static final String ADMIN = "admin";
    private static final String ADMIN_PASSWORD = "test-Admin-Passw0rd!";
    private static final String USER_PASSWORD = "Regular-Passw0rd!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IamRoleRepository roleRepository;

    @Autowired
    private IamAuditEventRepository auditEventRepository;

    @Test
    void adminCanCreateListUpdateRolesAndAssignThemToUsers() throws Exception {
        String suffix = idSuffix();
        String code = "auditor-" + suffix;
        MvcResult created = mockMvc.perform(post("/api/v1/roles")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "%s", "name": "审计员", "description": "只读审计"}
                                """.formatted(code)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.name").value("审计员"))
                .andExpect(jsonPath("$.description").value("只读审计"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        String roleId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/roles")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "%s", "name": "重复"}
                                """.formatted(code)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/roles").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.code == '%s')].name".formatted(code)).value("审计员"));

        mockMvc.perform(get("/api/v1/roles/" + roleId).with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roleId));

        mockMvc.perform(patch("/api/v1/roles/" + roleId)
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "高级审计员", "description": "更新后的描述"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("高级审计员"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        String username = "roleuser-" + suffix;
        String userId = createUser(username, "角色用户");

        mockMvc.perform(post("/api/v1/users/" + userId + "/roles")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roleId": "%s"}
                                """.formatted(roleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[?(@.code == '%s')].name".formatted(code)).value("高级审计员"));

        mockMvc.perform(get("/api/v1/users/" + userId).with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[?(@.id == '%s')].code".formatted(roleId)).value(code));

        mockMvc.perform(get("/api/v1/users/" + userId + "/roles").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == '%s')]".formatted(code)).isNotEmpty());

        mockMvc.perform(delete("/api/v1/users/" + userId + "/roles/" + roleId)
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/" + userId).with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[?(@.code == '%s')]".formatted(code)).isEmpty());

        assertThat(auditEventRepository.countByObjectIdAndEventType(UUID.fromString(roleId), "role.create"))
                .isEqualTo(1);
        assertThat(auditEventRepository.countByObjectIdAndEventType(UUID.fromString(userId), "user.assign_role"))
                .isEqualTo(1);
        assertThat(auditEventRepository.countByObjectIdAndEventType(UUID.fromString(userId), "user.remove_role"))
                .isEqualTo(1);
    }

    @Test
    void builtInAdminRoleCannotBeDeletedOrDisabledAndLastAdminCannotBeRemoved() throws Exception {
        UUID adminRoleId = roleRepository.findByCode("admin").orElseThrow().getId();

        mockMvc.perform(delete("/api/v1/roles/" + adminRoleId).with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/v1/roles/" + adminRoleId)
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "DISABLED"}
                                """))
                .andExpect(status().isConflict());

        String adminUserId = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/api/v1/me").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.profile.id");

        mockMvc.perform(delete("/api/v1/users/" + adminUserId + "/roles/" + adminRoleId)
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isConflict());
    }

    @Test
    void roleApisRejectAnonymousAndNonAdminUsers() throws Exception {
        String suffix = idSuffix();
        String user = "noroleadmin-" + suffix;
        createUser(user, "普通用户");

        mockMvc.perform(get("/api/v1/roles")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "x-%s", "name": "X"}
                                """.formatted(suffix)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/roles")
                        .with(httpBasic(user, USER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "blocked-%s", "name": "不应创建"}
                                """.formatted(suffix)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/roles").with(httpBasic(user, USER_PASSWORD)))
                .andExpect(status().isForbidden());
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
