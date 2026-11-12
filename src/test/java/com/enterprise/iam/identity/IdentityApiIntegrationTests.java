package com.enterprise.iam.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterprise.iam.audit.repo.IamAuditEventRepository;
import com.enterprise.iam.identity.domain.CredentialType;
import com.enterprise.iam.identity.domain.IamCredential;
import com.enterprise.iam.identity.repo.IamCredentialRepository;
import com.enterprise.iam.identity.repo.IamPasswordHistoryRepository;
import com.enterprise.iam.identity.service.PasswordService;
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
class IdentityApiIntegrationTests extends EmbeddedPostgresSupport {

    private static final String ADMIN = "admin";
    private static final String ADMIN_PASSWORD = "test-Admin-Passw0rd!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IamCredentialRepository credentialRepository;

    @Autowired
    private IamPasswordHistoryRepository passwordHistoryRepository;

    @Autowired
    private IamAuditEventRepository auditEventRepository;

    @Test
    void healthIsOpenAndApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void createUserEnforcesUniqueUsernameAndWritesAuditWithoutExposingHash() throws Exception {
        String username = "alice-" + UUID.randomUUID().toString().substring(0, 8);
        String body = """
                {
                  "username": "%s",
                  "displayName": "Alice Example",
                  "email": "alice@example.com",
                  "password": "Alice-Passw0rd!"
                }
                """.formatted(username);

        MvcResult created = mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.displayName").value("Alice Example"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.secretHash").doesNotExist())
                .andReturn();

        String id = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        UUID userId = UUID.fromString(id);

        mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());

        IamCredential credential = credentialRepository
                .findByUserIdAndType(userId, CredentialType.PASSWORD)
                .orElseThrow();
        assertThat(credential.getSecretHash()).isNotEqualTo("Alice-Passw0rd!");
        assertThat(PasswordService.looksLikeArgon2id(credential.getSecretHash())).isTrue();

        assertThat(auditEventRepository.countByObjectIdAndEventType(userId, "user.create")).isEqualTo(1);
    }

    @Test
    void adminCanUpdateStatusAssignOrgSetAttributesAndResetPassword() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        MvcResult orgResult = mockMvc.perform(post("/api/v1/orgs")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "eng-%s", "name": "Engineering", "sortOrder": 1}
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("eng-" + suffix))
                .andReturn();
        String orgId = com.jayway.jsonpath.JsonPath.read(orgResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/orgs/tree").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')].name".formatted(orgId)).value("Engineering"));

        MvcResult userResult = mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "bob-%s",
                                  "displayName": "Bob Example",
                                  "password": "Initial-Passw0rd!"
                                }
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andReturn();
        String userId = com.jayway.jsonpath.JsonPath.read(userResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/users/" + userId + "/assignments")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orgId": "%s", "primary": true}
                                """.formatted(orgId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryOrgId").value(orgId));

        mockMvc.perform(put("/api/v1/users/" + userId + "/attributes")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"attributes": [{"attrCode": "title", "valueText": "Engineer"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attributes[0].attrCode").value("title"))
                .andExpect(jsonPath("$.attributes[0].valueText").value("Engineer"));

        mockMvc.perform(post("/api/v1/users/" + userId + "/password")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPassword": "Reset-Passw0rd!", "mustChange": true}
                                """))
                .andExpect(status().isNoContent());

        IamCredential credential = credentialRepository
                .findByUserIdAndType(UUID.fromString(userId), CredentialType.PASSWORD)
                .orElseThrow();
        assertThat(PasswordService.looksLikeArgon2id(credential.getSecretHash())).isTrue();
        assertThat(credential.getSecretHash()).isNotEqualTo("Reset-Passw0rd!");
        assertThat(credential.isMustChange()).isTrue();
        assertThat(passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(UUID.fromString(userId)))
                .isNotEmpty();

        mockMvc.perform(post("/api/v1/me/password")
                        .with(httpBasic("bob-" + suffix, "Reset-Passw0rd!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"oldPassword": "Reset-Passw0rd!", "newPassword": "Changed-Passw0rd!"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/me").with(httpBasic("bob-" + suffix, "Changed-Passw0rd!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value("bob-" + suffix))
                .andExpect(jsonPath("$.mustChangePassword").value(false));

        mockMvc.perform(patch("/api/v1/users/" + userId)
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "DISABLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));

        mockMvc.perform(get("/api/v1/me").with(httpBasic("bob-" + suffix, "Changed-Passw0rd!")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/v1/users/" + userId)
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "TERMINATED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"));

        mockMvc.perform(get("/api/v1/users/" + userId).with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"));

        assertThat(auditEventRepository.countByObjectIdAndEventType(UUID.fromString(userId), "user.update"))
                .isGreaterThanOrEqualTo(2);
        assertThat(auditEventRepository.countByObjectIdAndEventType(
                        UUID.fromString(orgId), "org.create"))
                .isEqualTo(1);
    }

    @Test
    void importJobRecordCanBeCreatedAndRead() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/import-jobs")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "user_csv", "fileRef": "file://imports/users.csv"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(get("/api/v1/import-jobs/" + id).with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileRef").value("file://imports/users.csv"));
    }
}
