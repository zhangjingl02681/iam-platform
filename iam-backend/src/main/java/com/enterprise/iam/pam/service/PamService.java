package com.enterprise.iam.pam.service;

import com.enterprise.iam.audit.AuditService;
import com.enterprise.iam.common.exception.BusinessException;
import com.enterprise.iam.common.exception.NotFoundException;
import com.enterprise.iam.config.IamProperties;
import com.enterprise.iam.pam.domain.PamAccount;
import com.enterprise.iam.pam.domain.PamCheckout;
import com.enterprise.iam.pam.domain.PamLifecycleStatus;
import com.enterprise.iam.pam.domain.PamRequest;
import com.enterprise.iam.pam.domain.PamRequestStatus;
import com.enterprise.iam.pam.domain.PamSessionRecord;
import com.enterprise.iam.pam.domain.PamTarget;
import com.enterprise.iam.pam.repo.PamAccountRepository;
import com.enterprise.iam.pam.repo.PamCheckoutRepository;
import com.enterprise.iam.pam.repo.PamRequestRepository;
import com.enterprise.iam.pam.repo.PamSessionRecordRepository;
import com.enterprise.iam.pam.repo.PamTargetRepository;
import com.enterprise.iam.pam.web.dto.CreatePamAccountRequest;
import com.enterprise.iam.pam.web.dto.CreatePamRequestRequest;
import com.enterprise.iam.pam.web.dto.CreatePamTargetRequest;
import com.enterprise.iam.pam.web.dto.PamAccountResponse;
import com.enterprise.iam.pam.web.dto.PamCheckoutResponse;
import com.enterprise.iam.pam.web.dto.PamRequestResponse;
import com.enterprise.iam.pam.web.dto.PamSecretResponse;
import com.enterprise.iam.pam.web.dto.PamTargetResponse;
import com.enterprise.iam.vault.SecretVault;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PamService {

    private final PamTargetRepository targetRepository;
    private final PamAccountRepository accountRepository;
    private final PamRequestRepository requestRepository;
    private final PamCheckoutRepository checkoutRepository;
    private final PamSessionRecordRepository sessionRecordRepository;
    private final SecretVault secretVault;
    private final AuditService auditService;
    private final IamProperties properties;
    private final Clock clock;

    public PamService(
            PamTargetRepository targetRepository,
            PamAccountRepository accountRepository,
            PamRequestRepository requestRepository,
            PamCheckoutRepository checkoutRepository,
            PamSessionRecordRepository sessionRecordRepository,
            SecretVault secretVault,
            AuditService auditService,
            IamProperties properties,
            Clock clock) {
        this.targetRepository = targetRepository;
        this.accountRepository = accountRepository;
        this.requestRepository = requestRepository;
        this.checkoutRepository = checkoutRepository;
        this.sessionRecordRepository = sessionRecordRepository;
        this.secretVault = secretVault;
        this.auditService = auditService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public PamTargetResponse createTarget(CreatePamTargetRequest request) {
        PamTarget target = new PamTarget();
        target.setType(request.type().trim());
        target.setName(request.name().trim());
        target.setAddress(request.address());
        target.setEnv(request.env());
        target.setStatus(PamLifecycleStatus.ACTIVE);
        target = targetRepository.save(target);
        auditService.record(
                "pam.target.create",
                "create",
                "pam_target",
                target.getId(),
                "success",
                Map.of("name", target.getName(), "type", target.getType()));
        return PamTargetResponse.from(target);
    }

    public List<PamTargetResponse> listTargets() {
        return targetRepository.findAllByOrderByNameAsc().stream().map(PamTargetResponse::from).toList();
    }

    @Transactional
    public PamAccountResponse createAccount(UUID targetId, CreatePamAccountRequest request) {
        PamTarget target = targetRepository
                .findById(targetId)
                .orElseThrow(() -> new NotFoundException("pam_target_not_found", "PAM target not found"));
        if (target.getStatus() != PamLifecycleStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.CONFLICT, "pam_target_inactive", "Target is not active");
        }
        PamAccount account = new PamAccount();
        account.setTargetId(targetId);
        account.setUsername(request.username().trim());
        account.setRotateCron(request.rotateCron());
        account.setStatus(PamLifecycleStatus.ACTIVE);
        account = accountRepository.save(account);
        String vaultRef = "vault:pam/" + account.getId();
        secretVault.store(vaultRef, request.secret());
        account.setVaultRef(vaultRef);
        account = accountRepository.save(account);
        auditService.record(
                "pam.account.create",
                "create",
                "pam_account",
                account.getId(),
                "success",
                Map.of("targetId", targetId.toString(), "username", account.getUsername()));
        return PamAccountResponse.from(account);
    }

    public List<PamAccountResponse> listAccounts(UUID targetId) {
        if (!targetRepository.existsById(targetId)) {
            throw new NotFoundException("pam_target_not_found", "PAM target not found");
        }
        return accountRepository.findByTargetIdOrderByUsernameAsc(targetId).stream()
                .map(PamAccountResponse::from)
                .toList();
    }

    @Transactional
    public PamRequestResponse createRequest(UUID requesterId, CreatePamRequestRequest request) {
        PamAccount account = accountRepository
                .findById(request.accountId())
                .orElseThrow(() -> new NotFoundException("pam_account_not_found", "PAM account not found"));
        if (account.getStatus() != PamLifecycleStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.CONFLICT, "pam_account_inactive", "Account is not active");
        }
        if (!request.endAt().isAfter(request.startAt())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid_window", "End must be after start");
        }
        PamRequest entity = new PamRequest();
        entity.setAccountId(account.getId());
        entity.setRequesterId(requesterId);
        entity.setReason(request.reason().trim());
        entity.setStartAt(request.startAt());
        entity.setEndAt(request.endAt());
        entity.setStatus(PamRequestStatus.PENDING);
        entity = requestRepository.save(entity);
        auditService.record(
                "pam.request.create",
                "create",
                "pam_request",
                entity.getId(),
                "success",
                Map.of("accountId", account.getId().toString(), "reasonPresent", true));
        return PamRequestResponse.from(entity);
    }

    public List<PamRequestResponse> listRequests(UUID requesterId, boolean admin) {
        List<PamRequest> requests =
                admin ? requestRepository.findAllByOrderByCreatedAtDesc() : requestRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId);
        return requests.stream().map(PamRequestResponse::from).toList();
    }

    @Transactional
    public PamRequestResponse approve(UUID requestId, UUID approverId) {
        return decide(requestId, approverId, true, null);
    }

    @Transactional
    public PamRequestResponse deny(UUID requestId, UUID approverId, String reason) {
        return decide(requestId, approverId, false, reason);
    }

    private PamRequestResponse decide(UUID requestId, UUID approverId, boolean approved, String reason) {
        PamRequest request = requestRepository
                .findById(requestId)
                .orElseThrow(() -> new NotFoundException("pam_request_not_found", "PAM request not found"));
        if (request.getStatus() != PamRequestStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "pam_request_not_pending", "Request is not pending");
        }
        request.setStatus(approved ? PamRequestStatus.APPROVED : PamRequestStatus.REJECTED);
        request.setApproverId(approverId);
        request.setDecidedAt(clock.instant());
        requestRepository.save(request);
        auditService.record(
                approved ? "pam.request.approve" : "pam.request.deny",
                approved ? "approve" : "deny",
                "pam_request",
                request.getId(),
                "success",
                reason == null ? Map.of() : Map.of("reason", reason));
        return PamRequestResponse.from(request);
    }

    @Transactional
    public PamSecretResponse checkout(UUID requestId, UUID userId) {
        PamRequest request = requestRepository
                .findById(requestId)
                .orElseThrow(() -> new NotFoundException("pam_request_not_found", "PAM request not found"));
        if (!request.getRequesterId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "access_denied", "Access denied");
        }
        if (request.getStatus() != PamRequestStatus.APPROVED) {
            throw new BusinessException(HttpStatus.CONFLICT, "pam_request_not_approved", "Request is not approved");
        }
        Instant now = clock.instant();
        if (now.isBefore(request.getStartAt()) || now.isAfter(request.getEndAt())) {
            throw new BusinessException(HttpStatus.CONFLICT, "pam_window_closed", "Request is outside its time window");
        }
        var existing = checkoutRepository.findFirstByRequestIdAndCheckedInAtIsNullOrderByCheckedOutAtDesc(requestId);
        if (existing.isPresent() && existing.get().getExpiresAt().isAfter(now)) {
            throw new BusinessException(HttpStatus.CONFLICT, "pam_checkout_active", "An active checkout already exists");
        }
        PamAccount account = accountRepository
                .findById(request.getAccountId())
                .orElseThrow(() -> new NotFoundException("pam_account_not_found", "PAM account not found"));
        String secret = secretVault
                .load(account.getVaultRef())
                .orElseThrow(() -> new BusinessException(HttpStatus.CONFLICT, "pam_secret_missing", "Credential is not available"));
        Instant expiresAt = now.plus(properties.getPam().getCheckoutTtl());
        if (expiresAt.isAfter(request.getEndAt())) {
            expiresAt = request.getEndAt();
        }
        PamCheckout checkout = new PamCheckout();
        checkout.setRequestId(requestId);
        checkout.setAccountId(account.getId());
        checkout.setUserId(userId);
        checkout.setCheckedOutAt(now);
        checkout.setExpiresAt(expiresAt);
        checkout = checkoutRepository.save(checkout);
        PamSessionRecord record = new PamSessionRecord();
        record.setCheckoutId(checkout.getId());
        record.setStartedAt(now);
        record.setStorageUri(null);
        sessionRecordRepository.save(record);
        auditService.record(
                "pam.checkout",
                "checkout",
                "pam_checkout",
                checkout.getId(),
                "success",
                Map.of("requestId", requestId.toString(), "accountId", account.getId().toString()));
        return new PamSecretResponse(checkout.getId(), requestId, account.getUsername(), secret, expiresAt);
    }

    public List<PamCheckoutResponse> listCheckouts(UUID userId, boolean admin) {
        Instant now = clock.instant();
        List<PamCheckout> checkouts = admin
                ? checkoutRepository.findAll()
                : checkoutRepository.findByUserIdOrderByCheckedOutAtDesc(userId);
        return checkouts.stream().map(item -> PamCheckoutResponse.from(item, now)).toList();
    }

    @Transactional
    public void checkIn(UUID checkoutId, UUID userId, boolean admin) {
        PamCheckout checkout = checkoutRepository
                .findById(checkoutId)
                .orElseThrow(() -> new NotFoundException("pam_checkout_not_found", "Checkout not found"));
        if (!admin && !checkout.getUserId().equals(userId)) {
            throw new NotFoundException("pam_checkout_not_found", "Checkout not found");
        }
        Instant now = clock.instant();
        if (checkout.getCheckedInAt() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "pam_already_checked_in", "Checkout already checked in");
        }
        if (!checkout.getExpiresAt().isAfter(now)) {
            throw new BusinessException(HttpStatus.CONFLICT, "pam_checkout_expired", "Checkout has expired");
        }
        checkout.setCheckedInAt(now);
        checkoutRepository.save(checkout);
        for (PamSessionRecord record : sessionRecordRepository.findByCheckoutId(checkoutId)) {
            if (record.getEndedAt() == null) {
                record.setEndedAt(now);
                sessionRecordRepository.save(record);
            }
        }
        auditService.record(
                "pam.checkin",
                "checkin",
                "pam_checkout",
                checkout.getId(),
                "success",
                Map.of("requestId", checkout.getRequestId().toString()));
    }
}
