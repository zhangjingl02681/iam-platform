package com.enterprise.iam.oidc;

import com.enterprise.iam.auth.IamPrincipal;
import com.enterprise.iam.identity.service.MfaService;
import com.enterprise.iam.session.IamLoginSessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MfaLoginController {

    private final MfaService mfaService;
    private final IamLoginSessionService loginSessionService;
    private final SecurityContextRepository securityContextRepository;
    private final RequestCache requestCache = new HttpSessionRequestCache();

    public MfaLoginController(
            MfaService mfaService,
            IamLoginSessionService loginSessionService,
            SecurityContextRepository securityContextRepository) {
        this.mfaService = mfaService;
        this.loginSessionService = loginSessionService;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/login/mfa")
    public void complete(
            @RequestParam("code") String code, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String rawToken = rawToken(request);
        try {
            IamPrincipal principal = mfaService.completeChallenge(rawToken, code);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);
            loginSessionService.createForLogin(request, authentication, "pwd otp");
            clearPending(request, response);
            SavedRequest saved = requestCache.getRequest(request, response);
            if (saved != null) {
                response.sendRedirect(saved.getRedirectUrl());
            } else {
                response.sendRedirect(request.getContextPath() + "/");
            }
        } catch (BadCredentialsException ex) {
            response.sendRedirect(request.getContextPath() + "/login?mfa=required&error=mfa_invalid");
        }
    }

    static String rawToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object value = session.getAttribute(MfaService.PENDING_TOKEN_ATTR);
            if (value instanceof String token && !token.isBlank()) {
                return token;
            }
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (MfaService.PENDING_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void clearPending(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(MfaService.PENDING_TOKEN_ATTR);
        }
        Cookie cookie = new Cookie(MfaService.PENDING_COOKIE, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }
}
