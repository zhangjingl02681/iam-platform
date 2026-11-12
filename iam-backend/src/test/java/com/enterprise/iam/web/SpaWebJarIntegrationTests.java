package com.enterprise.iam.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterprise.iam.support.EmbeddedPostgresSupport;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpaWebJarIntegrationTests extends EmbeddedPostgresSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginAndIndexServeBuiltHtmlFromFrontendWebJar() throws Exception {
        ClassPathResource index = new ClassPathResource("META-INF/resources/index.html");
        assertThat(index.exists())
                .as("iam-frontend web jar must expose index.html at META-INF/resources/")
                .isTrue();
        byte[] expected = index.getContentAsByteArray();
        String expectedHtml = new String(expected, StandardCharsets.UTF_8);
        assertThat(expectedHtml).contains("<div id=\"root\">");
        assertThat(expectedHtml).contains("/assets/");
        assertThat(expectedHtml).doesNotContain("正在加载身份门户");

        mockMvc.perform(get("/login").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/html")))
                .andExpect(content().bytes(expected));

        mockMvc.perform(get("/index.html").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().bytes(expected));
    }
}
