package com.enterprise.iam.auth;

import com.enterprise.iam.identity.domain.UserStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class IamPrincipal implements UserDetails {

    private final UUID userId;
    private final String username;
    private final String displayName;
    private final String email;
    private final String passwordHash;
    private final UserStatus status;
    private final Instant lockedUntil;
    private final Collection<? extends GrantedAuthority> authorities;

    public IamPrincipal(
            UUID userId,
            String username,
            String displayName,
            String email,
            String passwordHash,
            UserStatus status,
            Instant lockedUntil,
            Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
        this.lockedUntil = lockedUntil;
        this.authorities = authorities;
    }

    public UUID getUserId() {
        return userId;
    }

    public UserStatus getStatus() {
        return status;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (lockedUntil != null && lockedUntil.isAfter(Instant.now())) {
            return false;
        }
        return status != UserStatus.LOCKED || (lockedUntil != null && !lockedUntil.isAfter(Instant.now()));
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status != UserStatus.DISABLED && status != UserStatus.TERMINATED;
    }
}
