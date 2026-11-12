package com.enterprise.iam.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionRevocationIntegrationTests extends EmbeddedPostgresSupport {

    private static final String ADMIN = "admin";
    private static final String ADMIN_PASSWORD = "test-Admin-Passw0rd!";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void revokingOneSessionImmediatelyRejectsItWhileOtherSessionsRemainValid() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "sess-hard-" + suffix;
        mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "displayName": "Session Hardening",
                                  "password": "Regular-Passw0rd!"
                                }
                                """.formatted(username)))
                .andExpect(status().isCreated());

        MockHttpSession first = formLogin(username, "Regular-Passw0rd!");
        MockHttpSession second = formLogin(username, "Regular-Passw0rd!");

        mockMvc.perform(get("/api/v1/me").session(first).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value(username));
        mockMvc.perform(get("/api/v1/me").session(second).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        MvcResult listed = mockMvc.perform(get("/api/v1/me/sessions").session(first))
                .andExpect(status().isOk())
                .andReturn();
        String firstId = findSessionId(listed.getResponse().getContentAsString(), true);
        String secondId = findSessionId(listed.getResponse().getContentAsString(), false);

        mockMvc.perform(post("/api/v1/sessions/" + secondId + "/revoke").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/me").session(second).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me").session(first).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value(username));

        mockMvc.perform(get("/api/v1/me/sessions").session(first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[?(@.id == '%s' && @.status == 'ACTIVE')]".formatted(firstId)).isNotEmpty())
                .andExpect(jsonPath("$.[?(@.id == '%s' && @.status == 'REVOKED')]".formatted(secondId)).isNotEmpty());
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

    private static String findSessionId(String json, boolean current) {
        net.minidev.json.JSONArray ids = com.jayway.jsonpath.JsonPath.read(
                json, "$.[?(@.current == " + current + " && @.status == 'ACTIVE')].id");
        assertThat(ids).isNotEmpty();
        return String.valueOf(ids.get(0));
    }
}
