package com.enterprise.iam.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterprise.iam.identity.domain.CredentialType;
import com.enterprise.iam.identity.domain.IamCredential;
import com.enterprise.iam.identity.domain.IamUser;
import com.enterprise.iam.identity.domain.UserStatus;
import com.enterprise.iam.identity.repo.IamCredentialRepository;
import com.enterprise.iam.identity.repo.IamUserRepository;
import com.enterprise.iam.identity.service.PasswordService;
import com.enterprise.iam.support.EmbeddedPostgresSupport;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImportJobParsingIntegrationTests extends EmbeddedPostgresSupport {

    private static final String ADMIN = "admin";
    private static final String ADMIN_PASSWORD = "test-Admin-Passw0rd!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IamUserRepository userRepository;

    @Autowired
    private IamCredentialRepository credentialRepository;

    @Test
    void csvUploadCreatesAndUpdatesUsersAndReportsRowFailures() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String orgCode = "import-org-" + suffix;
        mockMvc.perform(post("/api/v1/orgs")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "%s", "name": "导入组织", "sortOrder": 1}
                                """.formatted(orgCode)))
                .andExpect(status().isCreated());

        String existing = "exist-" + suffix;
        mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "displayName": "旧名称",
                                  "email": "old@example.com",
                                  "password": "Initial-Passw0rd!"
                                }
                                """.formatted(existing)))
                .andExpect(status().isCreated());

        String csv =
                """
                username,display_name,email,org_code,status,temporary_password
                newuser-%s,新用户,new@example.com,%s,active,Temp-Passw0rd1!
                %s,更新后的名称,updated@example.com,%s,active,
                badrow-%s,缺少组织,bad@example.com,missing-org-%s,active,Temp-Passw0rd1!
                ,没有用户名,x@example.com,%s,active,
                """.formatted(suffix, orgCode, existing, orgCode, suffix, suffix, orgCode);

        MockMultipartFile file = new MockMultipartFile(
                "file", "users.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        MvcResult created = mockMvc.perform(multipart("/api/v1/import-jobs")
                        .file(file)
                        .param("type", "user_csv")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("user_csv"))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.failedCount").value(2))
                .andExpect(jsonPath("$.finishedAt").isNotEmpty())
                .andExpect(jsonPath("$.errorReport").isString())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.temporaryPassword").doesNotExist())
                .andExpect(jsonPath("$.secretHash").doesNotExist())
                .andReturn();

        String body = created.getResponse().getContentAsString();
        assertThat(body).doesNotContain("Temp-Passw0rd1!");
        String errorReport = com.jayway.jsonpath.JsonPath.read(body, "$.errorReport");
        assertThat(errorReport).contains("missing-org-" + suffix);
        assertThat(errorReport).doesNotContain("Temp-Passw0rd1!");

        IamUser createdUser = userRepository.findByUsername("newuser-" + suffix).orElseThrow();
        assertThat(createdUser.getDisplayName()).isEqualTo("新用户");
        assertThat(createdUser.getEmail()).isEqualTo("new@example.com");
        assertThat(createdUser.getPrimaryOrgId()).isNotNull();
        IamCredential credential = credentialRepository
                .findByUserIdAndType(createdUser.getId(), CredentialType.PASSWORD)
                .orElseThrow();
        assertThat(credential.getSecretHash()).isNotEqualTo("Temp-Passw0rd1!");
        assertThat(PasswordService.looksLikeArgon2id(credential.getSecretHash())).isTrue();
        assertThat(credential.isMustChange()).isTrue();

        mockMvc.perform(get("/api/v1/users/" + createdUser.getId()).with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser-" + suffix))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.secretHash").doesNotExist());

        IamUser updated = userRepository.findByUsername(existing).orElseThrow();
        assertThat(updated.getDisplayName()).isEqualTo("更新后的名称");
        assertThat(updated.getEmail()).isEqualTo("updated@example.com");
        assertThat(updated.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(updated.getPrimaryOrgId()).isNotNull();

        assertThat(userRepository.findByUsername("badrow-" + suffix)).isEmpty();
    }

    @Test
    void jsonCsvBodyIsParsedAndMissingOrgFailsTheRow() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String csvContent =
                "username,display_name,email,org_code,status,temporary_password\n"
                        + "jsonuser-%s,JSON用户,json@example.com,no-such-org-%s,active,Temp-Passw0rd1!\n"
                                .formatted(suffix, suffix);
        String json = """
                {"type": "user_csv", "csv": %s}
                """.formatted(toJsonString(csvContent));

        mockMvc.perform(post("/api/v1/import-jobs")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.successCount").value(0))
                .andExpect(jsonPath("$.failedCount").value(1))
                .andExpect(jsonPath("$.finishedAt").isNotEmpty())
                .andExpect(jsonPath("$.errorReport").value(org.hamcrest.Matchers.containsString("no-such-org-" + suffix)));

        assertThat(userRepository.findByUsername("jsonuser-" + suffix)).isEmpty();
    }

    @Test
    void importJobsRejectAnonymousAndNonAdminUsers() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String csv = "username,display_name,email,org_code,status,temporary_password\n"
                + "denied-%s,Denied,denied@example.com,,active,Temp-Passw0rd1!\n".formatted(suffix);
        MockMultipartFile file = new MockMultipartFile(
                "file", "users.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/import-jobs").file(file).param("type", "user_csv"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/import-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "user_csv", "csv": "username,display_name\\n"}
                                """))
                .andExpect(status().isUnauthorized());

        String user = "importer-" + suffix;
        mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "displayName": "普通用户",
                                  "password": "Regular-Passw0rd!"
                                }
                                """.formatted(user)))
                .andExpect(status().isCreated());

        mockMvc.perform(multipart("/api/v1/import-jobs")
                        .file(file)
                        .param("type", "user_csv")
                        .with(httpBasic(user, "Regular-Passw0rd!")))
                .andExpect(status().isForbidden());
    }

    private static String toJsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
