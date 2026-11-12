package com.enterprise.iam.auth;

import com.enterprise.iam.identity.domain.CredentialType;
import com.enterprise.iam.identity.domain.IamCredential;
import com.enterprise.iam.identity.domain.IamRole;
import com.enterprise.iam.identity.domain.IamUser;
import com.enterprise.iam.identity.domain.RoleStatus;
import com.enterprise.iam.identity.repo.IamCredentialRepository;
import com.enterprise.iam.identity.repo.IamRoleRepository;
import com.enterprise.iam.identity.repo.IamUserRepository;
import com.enterprise.iam.identity.repo.IamUserRoleRepository;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IamUserDetailsService implements UserDetailsService {

    private final IamUserRepository userRepository;
    private final IamCredentialRepository credentialRepository;
    private final IamUserRoleRepository userRoleRepository;
    private final IamRoleRepository roleRepository;

    public IamUserDetailsService(
            IamUserRepository userRepository,
            IamCredentialRepository credentialRepository,
            IamUserRoleRepository userRoleRepository,
            IamRoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        IamUser user = userRepository.findByUsername(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        IamCredential credential = credentialRepository
                .findByUserIdAndType(user.getId(), CredentialType.PASSWORD)
                .orElseThrow(() -> new UsernameNotFoundException("Password credential not found"));
        List<SimpleGrantedAuthority> authorities = userRoleRepository.findByUserId(user.getId()).stream()
                .filter(assignment -> isEffective(assignment.getValidFrom(), assignment.getValidTo()))
                .map(assignment -> roleRepository.findById(assignment.getRoleId()).orElse(null))
                .filter(role -> role != null && role.getStatus() == RoleStatus.ACTIVE)
                .map(IamRole::getCode)
                .map(code -> new SimpleGrantedAuthority("ROLE_" + code.trim().toUpperCase(Locale.ROOT)))
                .toList();
        return new IamPrincipal(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                credential.getSecretHash(),
                user.getStatus(),
                credential.getLockedUntil(),
                authorities);
    }


    @Transactional(readOnly = true)
    public UserDetails loadUserById(UUID userId) {
        IamUser user = userRepository
                .findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return loadUserByUsername(user.getUsername());
    }

    @Transactional(readOnly = true)
    public String loadUserByIdOrUnknown(UUID userId) {
        return userRepository.findById(userId).map(IamUser::getUsername).orElse("user");
    }

    private boolean isEffective(Instant from, Instant to) {
        Instant now = Instant.now();
        if (from != null && now.isBefore(from)) {
            return false;
        }
        return to == null || !now.isAfter(to);
    }
}
