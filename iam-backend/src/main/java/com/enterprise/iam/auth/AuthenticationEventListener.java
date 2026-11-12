package com.enterprise.iam.auth;

import com.enterprise.iam.identity.domain.IamUser;
import com.enterprise.iam.identity.repo.IamUserRepository;
import com.enterprise.iam.identity.service.PasswordService;
import java.util.Locale;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationEventListener {

    private final IamUserRepository userRepository;
    private final PasswordService passwordService;

    public AuthenticationEventListener(IamUserRepository userRepository, PasswordService passwordService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        if (authentication.getPrincipal() instanceof IamPrincipal principal) {
            passwordService.registerSuccess(principal.getUserId());
        }
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        String name = event.getAuthentication().getName();
        if (name == null) {
            return;
        }
        userRepository.findByUsername(name.trim().toLowerCase(Locale.ROOT))
                .map(IamUser::getId)
                .ifPresent(passwordService::registerFailure);
    }
}
