package com.enterprise.iam.oidc;

import com.enterprise.iam.auth.IamPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OidcTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    @Override
    public void customize(JwtEncodingContext context) {
        if (!(context.getPrincipal() instanceof Authentication authentication)) {
            return;
        }
        if (!(authentication.getPrincipal() instanceof IamPrincipal principal)) {
            return;
        }
        boolean idToken = OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue());
        boolean accessToken = OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType());
        if (!idToken && !accessToken) {
            return;
        }
        context.getClaims().subject(principal.getUserId().toString());
        if (idToken && context.getAuthorizedScopes().contains(OidcScopes.PROFILE)) {
            context.getClaims().claim(StandardClaimNames.PREFERRED_USERNAME, principal.getUsername());
            if (StringUtils.hasText(principal.getDisplayName())) {
                context.getClaims().claim(StandardClaimNames.NAME, principal.getDisplayName());
            }
        }
        if (idToken
                && context.getAuthorizedScopes().contains(OidcScopes.EMAIL)
                && StringUtils.hasText(principal.getEmail())) {
            context.getClaims().claim(StandardClaimNames.EMAIL, principal.getEmail());
        }
    }
}
