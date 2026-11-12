package com.enterprise.iam.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    private static final String FALLBACK_HTML =
            """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>企业内部 IAM</title>
            </head>
            <body>
              <div id="root">正在加载身份门户…</div>
            </body>
            </html>
            """;

    @GetMapping({
        "/",
        "/portal",
        "/portal/",
        "/profile",
        "/sessions",
        "/password",
        "/consent",
        "/admin",
        "/login"
    })
    public void root(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeSpa(response);
    }

    @GetMapping({"/admin/{*path}", "/examples/{*path}", "/portal/{*path}"})
    public void nested(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeSpa(response);
    }

    private void writeSpa(HttpServletResponse response) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(new MediaType("text", "html", StandardCharsets.UTF_8).toString());
        ClassPathResource index = new ClassPathResource("static/index.html");
        if (index.exists() && index.isReadable()) {
            try (InputStream input = index.getInputStream()) {
                StreamUtils.copy(input, response.getOutputStream());
            }
            return;
        }
        response.getWriter().write(FALLBACK_HTML);
    }
}
