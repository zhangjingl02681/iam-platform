package com.enterprise.iam.oidc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class FormLoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        String error = "bad_credentials";
        if (exception instanceof LockedException) {
            error = "locked";
        } else if (exception instanceof DisabledException) {
            error = "disabled";
        }
        response.sendRedirect(request.getContextPath() + "/login?error=" + error);
    }
}
