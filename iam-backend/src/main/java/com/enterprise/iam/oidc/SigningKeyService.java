package com.enterprise.iam.oidc;

import com.enterprise.iam.audit.AuditService;
import com.enterprise.iam.common.exception.BusinessException;
import com.enterprise.iam.common.exception.NotFoundException;
import com.enterprise.iam.config.IamProperties;
import com.enterprise.iam.oidc.domain.IamSigningKey;
import com.enterprise.iam.oidc.domain.SigningKeyStatus;
import com.enterprise.iam.oidc.repo.IamSigningKeyRepository;
import com.enterprise.iam.oidc.web.SigningKeyResponse;
import com.enterprise.iam.oidc.web.SigningKeyRotationResponse;
import com.enterprise.iam.vault.SecretVault;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class SigningKeyService {

    private static final Logger log = LoggerFactory.getLogger(SigningKeyService.class);

    private final IamSigningKeyRepository repository;
    private final SecretVault secretVault;
    private final IamProperties properties;
    private final AuditService auditService;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final Object lock = new Object();

    public SigningKeyService(
            IamSigningKeyRepository repository,
            SecretVault secretVault,
            IamProperties properties,
            AuditService auditService,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.secretVault = secretVault;
        this.properties = properties;
        this.auditService = auditService;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void ensureCurrentKey() {
        transactionTemplate.executeWithoutResult(status -> {
        synchronized (lock) {
            if (repository.findByStatus(SigningKeyStatus.CURRENT).isPresent()) {
                return;
            }
            RSAKey imported = tryImportLegacyFile();
            if (imported != null) {
                persist(imported, SigningKeyStatus.CURRENT, null);
                log.info("Imported legacy signing JWK into the local vault");
                return;
            }
            persist(generate(), SigningKeyStatus.CURRENT, null);
            log.warn("Generated a development signing key in the local vault. Do not use this material in production.");
        }
        });
    }

    @Transactional
    public SigningKeyRotationResponse rotate() {
        synchronized (lock) {
            ensureCurrentKey();
            Instant now = clock.instant();
            IamSigningKey current = repository
                    .findByStatus(SigningKeyStatus.CURRENT)
                    .orElseThrow(() -> new IllegalStateException("No current signing key"));
            repository.findByStatus(SigningKeyStatus.PREVIOUS).ifPresent(previous -> {
                previous.setStatus(SigningKeyStatus.RETIRED);
                previous.setRetiredAt(now);
                previous.setValidUntil(now);
                repository.save(previous);
            });
            Instant validUntil = now.plus(properties.getOidc().getSigningKeyGrace());
            current.setStatus(SigningKeyStatus.PREVIOUS);
            current.setValidUntil(validUntil);
            repository.save(current);
            IamSigningKey created = persist(generate(), SigningKeyStatus.CURRENT, null);
            auditService.record(
                    "signing_key.rotate",
                    "rotate",
                    "signing_key",
                    created.getId(),
                    "success",
                    Map.of("currentKid", created.getKid(), "previousKid", current.getKid()));
            return new SigningKeyRotationResponse(SigningKeyResponse.from(created), SigningKeyResponse.from(current));
        }
    }

    @Transactional
    public SigningKeyResponse retire(String kid) {
        synchronized (lock) {
            IamSigningKey key = repository
                    .findByKid(kid)
                    .orElseThrow(() -> new NotFoundException("signing_key_not_found", "Signing key not found"));
            if (key.getStatus() == SigningKeyStatus.CURRENT) {
                throw new BusinessException(
                        HttpStatus.CONFLICT, "cannot_retire_current", "The current signing key cannot be retired");
            }
            Instant now = clock.instant();
            key.setStatus(SigningKeyStatus.RETIRED);
            key.setRetiredAt(now);
            key.setValidUntil(now);
            repository.save(key);
            auditService.record(
                    "signing_key.retire",
                    "retire",
                    "signing_key",
                    key.getId(),
                    "success",
                    Map.of("kid", key.getKid()));
            return SigningKeyResponse.from(key);
        }
    }

    public List<SigningKeyResponse> list() {
        ensureCurrentKey();
        return repository.findAllByOrderByCreatedAtDesc().stream().map(SigningKeyResponse::from).toList();
    }

    public RSAKey currentSigningKey() {
        ensureCurrentKey();
        IamSigningKey current = repository
                .findByStatus(SigningKeyStatus.CURRENT)
                .orElseThrow(() -> new IllegalStateException("No current signing key"));
        return loadPrivate(current);
    }

    public JWKSet jwkSetForValidation() {
        ensureCurrentKey();
        Instant now = clock.instant();
        List<JWK> keys = new ArrayList<>();
        for (IamSigningKey metadata : repository.findAllByOrderByCreatedAtDesc()) {
            if (usableForValidation(metadata, now)) {
                keys.add(loadPrivate(metadata).toPublicJWK());
            }
        }
        return new JWKSet(keys);
    }

    public JWKSet privateJwkSet() {
        List<JWK> keys = new ArrayList<>();
        keys.add(currentSigningKey());
        repository.findByStatus(SigningKeyStatus.PREVIOUS).ifPresent(previous -> {
            if (usableForValidation(previous, clock.instant())) {
                keys.add(loadPrivate(previous));
            }
        });
        return new JWKSet(keys);
    }

    private boolean usableForValidation(IamSigningKey key, Instant now) {
        if (key.getStatus() == SigningKeyStatus.RETIRED) {
            return false;
        }
        if (key.getStatus() == SigningKeyStatus.CURRENT) {
            return true;
        }
        if (key.getStatus() == SigningKeyStatus.PREVIOUS) {
            return key.getValidUntil() == null || key.getValidUntil().isAfter(now);
        }
        return false;
    }

    private IamSigningKey persist(RSAKey rsaKey, SigningKeyStatus status, Instant validUntil) {
        String kid = rsaKey.getKeyID();
        String vaultRef = "vault:signing/" + kid;
        secretVault.store(vaultRef, rsaKey.toJSONString());
        IamSigningKey entity = new IamSigningKey();
        entity.setKid(kid);
        entity.setVaultRef(vaultRef);
        entity.setStatus(status);
        entity.setValidUntil(validUntil);
        entity.setCreatedAt(clock.instant());
        return repository.save(entity);
    }

    private RSAKey loadPrivate(IamSigningKey metadata) {
        String json = secretVault
                .load(metadata.getVaultRef())
                .orElseThrow(() -> new IllegalStateException("Signing key material missing from vault: " + metadata.getKid()));
        try {
            JWK jwk = JWK.parse(json);
            if (!(jwk instanceof RSAKey rsaKey) || rsaKey.getPrivateExponent() == null) {
                throw new IllegalStateException("Vault material for " + metadata.getKid() + " is not a private RSA key");
            }
            return rsaKey;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to parse signing key " + metadata.getKid(), ex);
        }
    }

    private RSAKey generate() {
        try {
            return new RSAKeyGenerator(2048)
                    .keyID(UUID.randomUUID().toString())
                    .keyUse(KeyUse.SIGNATURE)
                    .generate();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate signing key", ex);
        }
    }

    private RSAKey tryImportLegacyFile() {
        String location = properties.getOidc().getJwkLocation();
        if (location == null || location.isBlank()) {
            return null;
        }
        Path path = Path.of(location).toAbsolutePath();
        if (!Files.exists(path)) {
            return null;
        }
        try {
            JWKSet existing = JWKSet.parse(Files.readString(path));
            if (existing.getKeys().isEmpty()) {
                return null;
            }
            JWK first = existing.getKeys().getFirst();
            if (first instanceof RSAKey rsaKey && rsaKey.getPrivateExponent() != null) {
                return rsaKey;
            }
        } catch (Exception ex) {
            log.warn("Unable to import legacy signing JWK from {}", path, ex);
        }
        return null;
    }
}
