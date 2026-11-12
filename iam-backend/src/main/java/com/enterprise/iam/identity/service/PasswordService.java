package com.enterprise.iam.identity.service;

import com.enterprise.iam.audit.AuditService;
import com.enterprise.iam.common.exception.BusinessException;
import com.enterprise.iam.common.exception.NotFoundException;
import com.enterprise.iam.config.IamProperties;
import com.enterprise.iam.identity.domain.CredentialType;
import com.enterprise.iam.identity.domain.IamCredential;
import com.enterprise.iam.identity.domain.IamPasswordHistory;
import com.enterprise.iam.identity.domain.IamUser;
import com.enterprise.iam.identity.domain.UserStatus;
import com.enterprise.iam.identity.repo.IamCredentialRepository;
import com.enterprise.iam.identity.repo.IamPasswordHistoryRepository;
import com.enterprise.iam.identity.repo.IamUserRepository;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordService {

    private static final Pattern HAS_LETTER = Pattern.compile(".*\\p{L}.*");
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");

    private final IamUserRepository userRepository;
    private final IamCredentialRepository credentialRepository;
    private final IamPasswordHistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;
    private final IamProperties properties;
    private final AuditService auditService;

    public PasswordService(
            IamUserRepository userRepository,
            IamCredentialRepository credentialRepository,
            IamPasswordHistoryRepository historyRepository,
            PasswordEncoder passwordEncoder,
            IamProperties properties,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.historyRepository = historyRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.auditService = auditService;
    }

    @Transactional
    public void setPassword(UUID userId, String rawPassword, boolean mustChange, boolean recordAudit) {
        IamUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("user_not_found", "User not found"));
        if (user.getStatus() == UserStatus.TERMINATED) {
            throw new BusinessException(HttpStatus.CONFLICT, "user_terminated", "Cannot set password for a terminated user");
        }
        validatePolicy(rawPassword);
        IamCredential credential = credentialRepository
                .findByUserIdAndType(userId, CredentialType.PASSWORD)
                .orElseGet(() -> {
                    IamCredential created = new IamCredential();
                    created.setUserId(userId);
                    created.setType(CredentialType.PASSWORD);
                    return created;
                });
        rejectReused(userId, rawPassword, credential.getSecretHash());
        if (credential.getSecretHash() != null) {
            appendHistory(userId, credential.getSecretHash());
        }
        credential.setSecretHash(passwordEncoder.encode(rawPassword));
        credential.setChangedAt(Instant.now());
        credential.setMustChange(mustChange);
        credential.setFailedCount(0);
        credential.setLockedUntil(null);
        credentialRepository.save(credential);
        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }
        if (recordAudit) {
            auditService.record("credential.password.set", "update", "user", userId, "success",
                    Map.of("mustChange", mustChange));
        }
    }

    @Transactional
    public void changeOwnPassword(UUID userId, String oldPassword, String newPassword) {
        IamCredential credential = credentialRepository
                .findByUserIdAndType(userId, CredentialType.PASSWORD)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST, "password_not_set", "No password credential is set"));
        if (!passwordEncoder.matches(oldPassword, credential.getSecretHash())) {
            registerFailure(userId);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid_old_password", "Current password is incorrect");
        }
        if (oldPassword.equals(newPassword)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "password_unchanged", "New password must be different");
        }
        setPassword(userId, newPassword, false, false);
        auditService.record("credential.password.change", "update", "user", userId, "success", Map.of());
    }

    @Transactional
    public void registerFailure(UUID userId) {
        IamCredential credential = credentialRepository
                .findByUserIdAndType(userId, CredentialType.PASSWORD)
                .orElse(null);
        if (credential == null) {
            return;
        }
        int failed = credential.getFailedCount() + 1;
        credential.setFailedCount(failed);
        int max = properties.getSecurity().getMaxFailedAttempts();
        IamUser user = userRepository.findById(userId).orElse(null);
        if (failed >= max && user != null && user.getStatus() == UserStatus.ACTIVE) {
            credential.setLockedUntil(Instant.now().plus(properties.getSecurity().getLockDuration()));
            user.setStatus(UserStatus.LOCKED);
            userRepository.save(user);
        }
        credentialRepository.save(credential);
    }

    @Transactional
    public void registerSuccess(UUID userId) {
        IamCredential credential = credentialRepository
                .findByUserIdAndType(userId, CredentialType.PASSWORD)
                .orElse(null);
        IamUser user = userRepository.findById(userId).orElse(null);
        if (credential == null || user == null) {
            return;
        }
        boolean dirty = false;
        if (credential.getFailedCount() > 0 || credential.getLockedUntil() != null) {
            credential.setFailedCount(0);
            credential.setLockedUntil(null);
            dirty = true;
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(UserStatus.ACTIVE);
        }
        Instant lastLogin = user.getLastLoginAt();
        if (lastLogin == null || lastLogin.isBefore(Instant.now().minusSeconds(15 * 60))) {
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
        } else if (user.getStatus() == UserStatus.ACTIVE) {
            userRepository.save(user);
        }
        if (dirty) {
            credentialRepository.save(credential);
        }
    }

    public boolean matches(String rawPassword, String secretHash) {
        return passwordEncoder.matches(rawPassword, secretHash);
    }

    public static void validatePolicy(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 12 || rawPassword.length() > 128) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "password_policy",
                    "Password must be between 12 and 128 characters");
        }
        if (!HAS_LETTER.matcher(rawPassword).matches() || !HAS_DIGIT.matcher(rawPassword).matches()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "password_policy",
                    "Password must contain at least one letter and one digit");
        }
    }

    private void rejectReused(UUID userId, String rawPassword, String currentHash) {
        if (currentHash != null && passwordEncoder.matches(rawPassword, currentHash)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "password_reused",
                    "Password was used recently and cannot be reused");
        }
        int keep = properties.getSecurity().getPasswordHistorySize();
        List<IamPasswordHistory> history = historyRepository.findByUserIdOrderByCreatedAtDesc(userId);
        history.stream().limit(keep).forEach(item -> {
            if (passwordEncoder.matches(rawPassword, item.getSecretHash())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "password_reused",
                        "Password was used recently and cannot be reused");
            }
        });
    }

    private void appendHistory(UUID userId, String secretHash) {
        IamPasswordHistory history = new IamPasswordHistory();
        history.setUserId(userId);
        history.setSecretHash(secretHash);
        historyRepository.save(history);
        List<IamPasswordHistory> all = historyRepository.findByUserIdOrderByCreatedAtDesc(userId);
        int keep = properties.getSecurity().getPasswordHistorySize();
        if (all.size() > keep) {
            historyRepository.deleteAll(all.subList(keep, all.size()));
        }
    }

    public static boolean looksLikeArgon2id(String hash) {
        return hash != null && (hash.startsWith("$argon2id$") || hash.contains("argon2id"));
    }

    public static String fingerprint(String hash) {
        return hash == null ? "" : HexFormat.of().formatHex(hash.getBytes(java.nio.charset.StandardCharsets.UTF_8)).substring(0, 8);
    }
}
