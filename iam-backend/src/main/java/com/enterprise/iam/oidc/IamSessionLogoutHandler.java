package com.enterprise.iam.oidc;

import com.enterprise.iam.session.IamLoginSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class IamSessionLogoutHandler implements LogoutHandler {

    private final IamLoginSessionService loginSessionService;

    public IamSessionLogoutHandler(IamLoginSessionService loginSessionService) {
        this.loginSessionService = loginSessionService;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        loginSessionService.currentSessionId(request)
                .ifPresent(sessionId -> loginSessionService.revoke(sessionId, "logout"));
    }
}
