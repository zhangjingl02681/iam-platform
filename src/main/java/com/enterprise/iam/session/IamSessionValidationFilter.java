package com.enterprise.iam.session;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class IamSessionValidationFilter extends OncePerRequestFilter {

    private final IamLoginSessionService loginSessionService;

    public IamSessionValidationFilter(IamLoginSessionService loginSessionService) {
        this.loginSessionService = loginSessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        loginSessionService.currentSessionId(request).ifPresent(sessionId -> {
            if (!loginSessionService.isActive(sessionId)) {
                SecurityContextHolder.clearContext();
                HttpSession httpSession = request.getSession(false);
                if (httpSession != null) {
                    try {
                        httpSession.invalidate();
                    } catch (IllegalStateException ignored) {
                        // already invalidated
                    }
                }
            }
        });
        filterChain.doFilter(request, response);
    }
}
