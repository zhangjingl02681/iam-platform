package com.enterprise.iam.oidc;

import com.enterprise.iam.auth.IamPrincipal;
import com.enterprise.iam.config.IamProperties;
import com.enterprise.iam.identity.service.MfaService;
import com.enterprise.iam.session.IamLoginSessionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
public class FormLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final IamLoginSessionService loginSessionService;
    private final MfaService mfaService;
    private final SecurityContextRepository securityContextRepository;
    private final IamProperties properties;

    public FormLoginSuccessHandler(
            IamLoginSessionService loginSessionService,
            MfaService mfaService,
            SecurityContextRepository securityContextRepository,
            IamProperties properties) {
        this.loginSessionService = loginSessionService;
        this.mfaService = mfaService;
        this.securityContextRepository = securityContextRepository;
        this.properties = properties;
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(false);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws ServletException, IOException {
        if (authentication.getPrincipal() instanceof IamPrincipal principal
                && mfaService.requiresSecondFactor(principal.getUserId())) {
            String rawToken = mfaService.createChallenge(principal.getUserId());
            SecurityContext empty = SecurityContextHolder.createEmptyContext();
            SecurityContextHolder.setContext(empty);
            securityContextRepository.saveContext(empty, request, response);
            request.getSession(true).setAttribute(MfaService.PENDING_TOKEN_ATTR, rawToken);
            Cookie cookie = new Cookie(MfaService.PENDING_COOKIE, rawToken);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge((int) properties.getMfa().getPendingTtl().toSeconds());
            response.addCookie(cookie);
            response.sendRedirect(request.getContextPath() + "/login?mfa=required");
            return;
        }
        loginSessionService.createForLogin(request, authentication);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
