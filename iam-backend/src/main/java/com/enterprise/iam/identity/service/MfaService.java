package com.enterprise.iam.identity.service;

import com.enterprise.iam.audit.AuditService;
import com.enterprise.iam.auth.IamPrincipal;
import com.enterprise.iam.auth.IamUserDetailsService;
import com.enterprise.iam.common.exception.BusinessException;
import com.enterprise.iam.common.exception.NotFoundException;
import com.enterprise.iam.config.IamProperties;
import com.enterprise.iam.identity.domain.IamMfaChallenge;
import com.enterprise.iam.identity.domain.IamMfaFactor;
import com.enterprise.iam.identity.domain.IamMfaRecoveryCode;
import com.enterprise.iam.identity.repo.IamMfaChallengeRepository;
import com.enterprise.iam.identity.repo.IamMfaFactorRepository;
import com.enterprise.iam.identity.repo.IamMfaRecoveryCodeRepository;
import com.enterprise.iam.identity.web.dto.MfaConfirmResponse;
import com.enterprise.iam.identity.web.dto.MfaEnrollmentResponse;
import com.enterprise.iam.identity.web.dto.MfaFactorResponse;
import com.enterprise.iam.oidc.TokenHashes;
import com.enterprise.iam.vault.SecretVault;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MfaService {

    public static final String PENDING_TOKEN_ATTR = "IAM_MFA_PENDING_TOKEN";
    public static final String PENDING_COOKIE = "IAM_MFA_PENDING";

    private static final String TYPE_TOTP = "totp";
    private static final char[] RECOVERY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final IamMfaFactorRepository factorRepository;
    private final IamMfaRecoveryCodeRepository recoveryCodeRepository;
    private final IamMfaChallengeRepository challengeRepository;
    private final TotpService totpService;
    private final SecretVault secretVault;
    private final AuditService auditService;
    private final IamProperties properties;
    private final Clock clock;
    private final IamUserDetailsService userDetailsService;
    private final SecureRandom random = new SecureRandom();

    public MfaService(
            IamMfaFactorRepository factorRepository,
            IamMfaRecoveryCodeRepository recoveryCodeRepository,
            IamMfaChallengeRepository challengeRepository,
            TotpService totpService,
            SecretVault secretVault,
            AuditService auditService,
            IamProperties properties,
            Clock clock,
            IamUserDetailsService userDetailsService) {
        this.factorRepository = factorRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.challengeRepository = challengeRepository;
        this.totpService = totpService;
        this.secretVault = secretVault;
        this.auditService = auditService;
        this.properties = properties;
        this.clock = clock;
        this.userDetailsService = userDetailsService;
    }

    public boolean requiresSecondFactor(UUID userId) {
        return !factorRepository.findByUserIdAndDisabledAtIsNullAndVerifiedAtIsNotNull(userId).isEmpty();
    }

    @Transactional
    public MfaEnrollmentResponse startTotpEnrollment(UUID userId, String label) {
        for (IamMfaFactor pending : factorRepository.findByUserIdAndDisabledAtIsNullAndVerifiedAtIsNull(userId)) {
            pending.setDisabledAt(clock.instant());
            factorRepository.save(pending);
        }
        String secret = totpService.generateSecret();
        IamMfaFactor factor = new IamMfaFactor();
        factor.setUserId(userId);
        factor.setType(TYPE_TOTP);
        factor.setLabel(label == null || label.isBlank() ? "Authenticator" : label.trim());
        factor.setCreatedAt(clock.instant());
        factor = factorRepository.save(factor);
        String vaultRef = "vault:mfa/" + factor.getId();
        secretVault.store(vaultRef, secret);
        factor.setSecretEnc(vaultRef);
        factor = factorRepository.save(factor);
        auditService.record(
                "mfa.enroll",
                "create",
                "mfa_factor",
                factor.getId(),
                "success",
                Map.of("type", TYPE_TOTP, "userId", userId.toString()));
        String account = userDetailsService.loadUserByIdOrUnknown(userId);
        String otpauth = totpService.otpauthUri(properties.getMfa().getIssuer(), account, secret);
        return new MfaEnrollmentResponse(
                factor.getId(), factor.getType(), factor.getLabel(), factor.getVerifiedAt(), secret, otpauth);
    }

    @Transactional
    public MfaConfirmResponse confirmEnrollment(UUID userId, UUID factorId, String code) {
        IamMfaFactor factor = factorRepository
                .findById(factorId)
                .orElseThrow(() -> new NotFoundException("mfa_factor_not_found", "MFA factor not found"));
        if (!factor.getUserId().equals(userId) || factor.getDisabledAt() != null) {
            throw new NotFoundException("mfa_factor_not_found", "MFA factor not found");
        }
        if (factor.getVerifiedAt() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "mfa_already_verified", "This factor is already verified");
        }
        String secret = loadSecret(factor);
        if (!totpService.verify(secret, code)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "mfa_invalid_code", "Invalid verification code");
        }
        factor.setVerifiedAt(clock.instant());
        factorRepository.save(factor);
        List<String> recoveryCodes = generateRecoveryCodes(userId, factor.getId());
        auditService.record(
                "mfa.verify",
                "verify",
                "mfa_factor",
                factor.getId(),
                "success",
                Map.of("type", TYPE_TOTP, "userId", userId.toString()));
        return new MfaConfirmResponse(MfaFactorResponse.from(factor), recoveryCodes);
    }

    public List<MfaFactorResponse> listFactors(UUID userId) {
        return factorRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(MfaFactorResponse::from)
                .toList();
    }

    @Transactional
    public void disableFactor(UUID actorId, UUID userId, UUID factorId, boolean admin) {
        IamMfaFactor factor = factorRepository
                .findById(factorId)
                .orElseThrow(() -> new NotFoundException("mfa_factor_not_found", "MFA factor not found"));
        if (!factor.getUserId().equals(userId)) {
            throw new NotFoundException("mfa_factor_not_found", "MFA factor not found");
        }
        if (!admin && !factor.getUserId().equals(actorId)) {
            throw new NotFoundException("mfa_factor_not_found", "MFA factor not found");
        }
        if (factor.getDisabledAt() == null) {
            factor.setDisabledAt(clock.instant());
            factorRepository.save(factor);
            auditService.record(
                    "mfa.disable",
                    "disable",
                    "mfa_factor",
                    factor.getId(),
                    "success",
                    Map.of("userId", userId.toString(), "admin", admin));
        }
    }

    @Transactional
    public String createChallenge(UUID userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = HexFormat.of().formatHex(bytes);
        IamMfaChallenge challenge = new IamMfaChallenge();
        challenge.setUserId(userId);
        challenge.setTokenHash(TokenHashes.sha256(raw));
        Instant now = clock.instant();
        challenge.setCreatedAt(now);
        challenge.setExpiresAt(now.plus(properties.getMfa().getPendingTtl()));
        challengeRepository.save(challenge);
        return raw;
    }

    @Transactional
    public IamPrincipal completeChallenge(String rawToken, String code) {
        if (rawToken == null || rawToken.isBlank() || code == null || code.isBlank()) {
            throw new BadCredentialsException("Invalid MFA challenge");
        }
        IamMfaChallenge challenge = challengeRepository
                .findByTokenHash(TokenHashes.sha256(rawToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid MFA challenge"));
        Instant now = clock.instant();
        if (challenge.getConsumedAt() != null || now.isAfter(challenge.getExpiresAt())) {
            throw new BadCredentialsException("Invalid MFA challenge");
        }
        if (!verifyLoginCode(challenge.getUserId(), code)) {
            auditService.record(
                    "mfa.login",
                    "verify",
                    "user",
                    challenge.getUserId(),
                    "failure",
                    Map.of("reason", "invalid_code"));
            throw new BadCredentialsException("Invalid MFA challenge");
        }
        challenge.setConsumedAt(now);
        challengeRepository.save(challenge);
        UserDetails details = userDetailsService.loadUserById(challenge.getUserId());
        auditService.record(
                "mfa.login",
                "verify",
                "user",
                challenge.getUserId(),
                "success",
                Map.of());
        return (IamPrincipal) details;
    }

    private boolean verifyLoginCode(UUID userId, String code) {
        String trimmed = code.trim().replace(" ", "");
        if (trimmed.matches("\\d{6}")) {
            for (IamMfaFactor factor :
                    factorRepository.findByUserIdAndDisabledAtIsNullAndVerifiedAtIsNotNull(userId)) {
                if (TYPE_TOTP.equals(factor.getType()) && totpService.verify(loadSecret(factor), trimmed)) {
                    return true;
                }
            }
        }
        String normalized = normalizeRecovery(trimmed);
        for (IamMfaRecoveryCode recovery : recoveryCodeRepository.findByUserIdAndUsedAtIsNull(userId)) {
            if (recovery.getCodeHash().equals(TokenHashes.sha256(normalized))) {
                recovery.setUsedAt(clock.instant());
                recoveryCodeRepository.save(recovery);
                return true;
            }
        }
        return false;
    }

    private List<String> generateRecoveryCodes(UUID userId, UUID factorId) {
        int count = Math.max(8, properties.getMfa().getRecoveryCodeCount());
        List<String> codes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String raw = randomRecovery();
            IamMfaRecoveryCode entity = new IamMfaRecoveryCode();
            entity.setUserId(userId);
            entity.setFactorId(factorId);
            entity.setCodeHash(TokenHashes.sha256(normalizeRecovery(raw)));
            entity.setCreatedAt(clock.instant());
            recoveryCodeRepository.save(entity);
            codes.add(raw);
        }
        return codes;
    }

    private String loadSecret(IamMfaFactor factor) {
        String ref = factor.getSecretEnc();
        return secretVault
                .load(ref)
                .orElseThrow(() -> new IllegalStateException("MFA secret missing from vault"));
    }

    private String randomRecovery() {
        char[] chars = new char[8];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = RECOVERY_ALPHABET[random.nextInt(RECOVERY_ALPHABET.length)];
        }
        return new String(chars, 0, 4) + "-" + new String(chars, 4, 4);
    }

    private static String normalizeRecovery(String value) {
        return value.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
    }
}
