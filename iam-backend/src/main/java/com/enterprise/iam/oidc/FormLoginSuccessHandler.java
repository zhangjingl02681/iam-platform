package com.enterprise.iam.oidc;

import com.enterprise.iam.session.IamLoginSessionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class FormLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final IamLoginSessionService loginSessionService;

    public FormLoginSuccessHandler(IamLoginSessionService loginSessionService) {
        this.loginSessionService = loginSessionService;
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(false);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws ServletException, IOException {
        loginSessionService.createForLogin(request, authentication);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
