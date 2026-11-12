package com.enterprise.iam.pam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterprise.iam.audit.domain.IamAuditEvent;
import com.enterprise.iam.audit.repo.IamAuditEventRepository;
import com.enterprise.iam.pam.domain.PamAccount;
import com.enterprise.iam.pam.domain.PamCheckout;
import com.enterprise.iam.pam.repo.PamAccountRepository;
import com.enterprise.iam.pam.repo.PamCheckoutRepository;
import com.enterprise.iam.pam.repo.PamSessionRecordRepository;
import com.enterprise.iam.support.EmbeddedPostgresSupport;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PamApiIntegrationTests extends EmbeddedPostgresSupport {

    private static final String ADMIN = "admin";
    private static final String ADMIN_PASSWORD = "test-Admin-Passw0rd!";
    private static final String SECRET = "Super-Secret-Root-Passw0rd!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PamAccountRepository accountRepository;

    @Autowired
    private PamCheckoutRepository checkoutRepository;

    @Autowired
    private PamSessionRecordRepository sessionRecordRepository;

    @Autowired
    private IamAuditEventRepository auditEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void requestApproveCheckoutShowsSecretOnceAndDoesNotPersistItInBusinessTables() throws Exception {
        UserFixture requester = createUser("pam-req-");
        String targetId = createTarget();
        String accountId = createAccount(targetId);

        PamAccount account = accountRepository.findById(UUID.fromString(accountId)).orElseThrow();
        assertThat(account.getVaultRef()).isNotBlank().isNotEqualTo(SECRET);
        assertThat(countSecretInBusinessTables()).isZero();

        Instant start = Instant.now().minus(5, ChronoUnit.MINUTES);
        Instant end = Instant.now().plus(2, ChronoUnit.HOURS);
        MvcResult requested = mockMvc.perform(post("/api/v1/pam/requests")
                        .with(httpBasic(requester.username(), requester.password()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "%s",
                                  "reason": "紧急检修生产库",
                                  "startAt": "%s",
                                  "endAt": "%s"
                                }
                                """.formatted(accountId, start, end)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.reason").value("紧急检修生产库"))
                .andExpect(jsonPath("$.secret").doesNotExist())
                .andReturn();
        String requestId = com.jayway.jsonpath.JsonPath.read(requested.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/pam/requests/" + requestId + "/checkout")
                        .with(httpBasic(requester.username(), requester.password())))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/pam/requests/" + requestId + "/approve")
                        .with(httpBasic(requester.username(), requester.password())))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/pam/requests/" + requestId + "/approve")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.secret").doesNotExist());

        MvcResult checkout = mockMvc.perform(post("/api/v1/pam/requests/" + requestId + "/checkout")
                        .with(httpBasic(requester.username(), requester.password())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").value(SECRET))
                .andExpect(jsonPath("$.username").value("root"))
                .andExpect(jsonPath("$.checkoutId").exists())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andReturn();
        String checkoutId = com.jayway.jsonpath.JsonPath.read(checkout.getResponse().getContentAsString(), "$.checkoutId");

        assertThat(countSecretInBusinessTables()).isZero();
        PamCheckout stored = checkoutRepository.findById(UUID.fromString(checkoutId)).orElseThrow();
        assertThat(stored.getCheckedInAt()).isNull();
        assertThat(sessionRecordRepository.findByCheckoutId(UUID.fromString(checkoutId))).isNotEmpty();

        mockMvc.perform(get("/api/v1/pam/checkouts").with(httpBasic(requester.username(), requester.password())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')].secret".formatted(checkoutId)).isEmpty())
                .andExpect(jsonPath("$[?(@.id == '%s')].status".formatted(checkoutId)).value("ACTIVE"));

        mockMvc.perform(post("/api/v1/pam/requests/" + requestId + "/checkout")
                        .with(httpBasic(requester.username(), requester.password())))
                .andExpect(status().isConflict());

        List<IamAuditEvent> checkoutAudits =
                auditEventRepository.findByObjectIdAndEventTypeOrderByOccurredAtAsc(UUID.fromString(checkoutId), "pam.checkout");
        assertThat(checkoutAudits).isNotEmpty();
        assertThat(String.valueOf(checkoutAudits.getFirst().getDetail())).doesNotContain(SECRET);

        mockMvc.perform(post("/api/v1/pam/checkouts/" + checkoutId + "/check-in")
                        .with(httpBasic(requester.username(), requester.password())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/pam/checkouts").with(httpBasic(requester.username(), requester.password())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')].status".formatted(checkoutId)).value("CHECKED_IN"));
    }

    @Test
    void adminCanDenyRequestAndExpiredCheckoutCannotBeUsed() throws Exception {
        UserFixture requester = createUser("pam-deny-");
        String targetId = createTarget();
        String accountId = createAccount(targetId);
        Instant start = Instant.now().minus(1, ChronoUnit.MINUTES);
        Instant end = Instant.now().plus(1, ChronoUnit.HOURS);
        String requestId = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(post("/api/v1/pam/requests")
                                .with(httpBasic(requester.username(), requester.password()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "accountId": "%s",
                                          "reason": "演练",
                                          "startAt": "%s",
                                          "endAt": "%s"
                                        }
                                        """.formatted(accountId, start, end)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id");

        mockMvc.perform(post("/api/v1/pam/requests/" + requestId + "/deny")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "时间窗口不合适"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(post("/api/v1/pam/requests/" + requestId + "/checkout")
                        .with(httpBasic(requester.username(), requester.password())))
                .andExpect(status().isConflict());

        String approvedId = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(post("/api/v1/pam/requests")
                                .with(httpBasic(requester.username(), requester.password()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "accountId": "%s",
                                          "reason": "第二次申请",
                                          "startAt": "%s",
                                          "endAt": "%s"
                                        }
                                        """.formatted(accountId, start, end)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id");
        mockMvc.perform(post("/api/v1/pam/requests/" + approvedId + "/approve")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk());
        String checkoutId = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(post("/api/v1/pam/requests/" + approvedId + "/checkout")
                                .with(httpBasic(requester.username(), requester.password())))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.checkoutId");

        PamCheckout checkout = checkoutRepository.findById(UUID.fromString(checkoutId)).orElseThrow();
        checkout.setExpiresAt(Instant.now().minusSeconds(5));
        checkoutRepository.save(checkout);

        mockMvc.perform(get("/api/v1/pam/checkouts").with(httpBasic(requester.username(), requester.password())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')].status".formatted(checkoutId)).value("EXPIRED"))
                .andExpect(jsonPath("$[?(@.id == '%s')].secret".formatted(checkoutId)).isEmpty());

        mockMvc.perform(post("/api/v1/pam/checkouts/" + checkoutId + "/check-in")
                        .with(httpBasic(requester.username(), requester.password())))
                .andExpect(status().isConflict());
    }

    @Test
    void unauthorizedUsersCannotRegisterTargetsOrSeeOthersRequests() throws Exception {
        UserFixture requester = createUser("pam-unauth-");
        UserFixture other = createUser("pam-other-");
        mockMvc.perform(post("/api/v1/pam/targets")
                        .with(httpBasic(requester.username(), requester.password()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "host", "name": "should-fail", "address": "10.0.0.1", "env": "prod"}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/pam/targets")).andExpect(status().isUnauthorized());

        String targetId = createTarget();
        String accountId = createAccount(targetId);
        Instant start = Instant.now().minus(1, ChronoUnit.MINUTES);
        Instant end = Instant.now().plus(1, ChronoUnit.HOURS);
        String requestId = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(post("/api/v1/pam/requests")
                                .with(httpBasic(requester.username(), requester.password()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "accountId": "%s",
                                          "reason": "本人申请",
                                          "startAt": "%s",
                                          "endAt": "%s"
                                        }
                                        """.formatted(accountId, start, end)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id");

        mockMvc.perform(get("/api/v1/pam/requests").with(httpBasic(other.username(), other.password())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(requestId)).isEmpty());

        mockMvc.perform(post("/api/v1/pam/requests/" + requestId + "/approve")
                        .with(httpBasic(other.username(), other.password())))
                .andExpect(status().isForbidden());
    }

    private long countSecretInBusinessTables() {
        Long accounts = jdbcTemplate.queryForObject(
                "select count(*) from pam_account where vault_ref = ? or username = ?",
                Long.class,
                SECRET,
                SECRET);
        Long requests = jdbcTemplate.queryForObject(
                "select count(*) from pam_request where reason = ?", Long.class, SECRET);
        return (accounts == null ? 0 : accounts) + (requests == null ? 0 : requests);
    }

    private String createTarget() throws Exception {
        String name = "prod-db-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult result = mockMvc.perform(post("/api/v1/pam/targets")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "host", "name": "%s", "address": "10.1.2.3", "env": "prod"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String createAccount(String targetId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/pam/targets/" + targetId + "/accounts")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "root", "secret": "%s"}
                                """.formatted(SECRET)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("root"))
                .andExpect(jsonPath("$.vaultRef").isNotEmpty())
                .andExpect(jsonPath("$.secret").doesNotExist())
                .andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        String vaultRef = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.vaultRef");
        assertThat(vaultRef).isNotEqualTo(SECRET);
        return id;
    }

    private UserFixture createUser(String prefix) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = prefix + suffix;
        String password = "Regular-Passw0rd!";
        MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "displayName": "PAM User",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return new UserFixture(id, username, password);
    }

    private record UserFixture(String id, String username, String password) {}
}
