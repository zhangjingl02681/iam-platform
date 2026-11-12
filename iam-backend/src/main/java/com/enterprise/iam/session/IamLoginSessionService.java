package com.enterprise.iam.session;

import com.enterprise.iam.auth.IamPrincipal;
import com.enterprise.iam.config.IamProperties;
import com.enterprise.iam.oidc.domain.IamOAuthAuthorization;
import com.enterprise.iam.oidc.repo.IamOAuthAuthorizationRepository;
import com.enterprise.iam.session.domain.IamRefreshToken;
import com.enterprise.iam.session.domain.IamRevocation;
import com.enterprise.iam.session.domain.IamSession;
import com.enterprise.iam.session.domain.SessionStatus;
import com.enterprise.iam.session.repo.IamRefreshTokenRepository;
import com.enterprise.iam.session.repo.IamRevocationRepository;
import com.enterprise.iam.session.repo.IamSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IamLoginSessionService {

    public static final String SESSION_ATTRIBUTE = "IAM_SESSION_ID";

    private final IamSessionRepository sessionRepository;
    private final IamRefreshTokenRepository refreshTokenRepository;
    private final IamRevocationRepository revocationRepository;
    private final IamOAuthAuthorizationRepository authorizationRepository;
    private final IamProperties properties;
    private final IamServletSessionRegistry servletSessionRegistry;

    public IamLoginSessionService(
            IamSessionRepository sessionRepository,
            IamRefreshTokenRepository refreshTokenRepository,
            IamRevocationRepository revocationRepository,
            IamOAuthAuthorizationRepository authorizationRepository,
            IamProperties properties,
            IamServletSessionRegistry servletSessionRegistry) {
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.revocationRepository = revocationRepository;
        this.authorizationRepository = authorizationRepository;
        this.properties = properties;
        this.servletSessionRegistry = servletSessionRegistry;
    }

    @Transactional
    public IamSession createForLogin(HttpServletRequest request, Authentication authentication) {
        return createForLogin(request, authentication, "pwd");
    }

    @Transactional
    public IamSession createForLogin(HttpServletRequest request, Authentication authentication, String amr) {
        if (!(authentication.getPrincipal() instanceof IamPrincipal principal)) {
            return null;
        }
        Instant now = Instant.now();
        IamSession session = new IamSession();
        session.setUserId(principal.getUserId());
        session.setAmr(amr == null || amr.isBlank() ? "pwd" : amr);
        session.setIp(resolveIp(request, authentication));
        session.setUserAgent(trim(request.getHeader("User-Agent"), 512));
        session.setCreatedAt(now);
        session.setLastSeenAt(now);
        session.setIdleExpiresAt(now.plus(properties.getOidc().getSessionIdleTimeout()));
        session.setAbsExpiresAt(now.plus(properties.getOidc().getSessionAbsoluteTimeout()));
        session.setStatus(SessionStatus.ACTIVE);
        session = sessionRepository.save(session);
        var httpSession = request.getSession(true);
        httpSession.setAttribute(SESSION_ATTRIBUTE, session.getId());
        servletSessionRegistry.bind(session.getId(), httpSession);
        return session;
    }

    public Optional<UUID> currentSessionId(HttpServletRequest request) {
        HttpSession httpSession;
        try {
            httpSession = request.getSession(false);
        } catch (IllegalStateException ex) {
            return Optional.empty();
        }
        if (httpSession == null) {
            return Optional.empty();
        }
        Object value;
        try {
            value = httpSession.getAttribute(SESSION_ATTRIBUTE);
        } catch (IllegalStateException ex) {
            return Optional.empty();
        }
        if (value instanceof UUID id) {
            return Optional.of(id);
        }
        if (value instanceof String text) {
            return Optional.of(UUID.fromString(text));
        }
        return Optional.empty();
    }

    public List<IamSession> listForUser(UUID userId) {
        return sessionRepository.findByUserIdOrderByLastSeenAtDesc(userId);
    }

    public Page<IamSession> search(UUID userId, SessionStatus status, Pageable pageable) {
        if (userId != null && status != null) {
            return sessionRepository.findByUserIdAndStatus(userId, status, pageable);
        }
        if (userId != null) {
            return sessionRepository.findByUserId(userId, pageable);
        }
        if (status != null) {
            return sessionRepository.findByStatus(status, pageable);
        }
        return sessionRepository.findAll(pageable);
    }

    public boolean isActive(UUID sessionId) {
        if (sessionId == null) {
            return false;
        }
        Boolean cached = servletSessionRegistry.cachedActive(sessionId);
        if (cached != null && !cached) {
            return false;
        }
        boolean active = sessionRepository.findById(sessionId)
                .map(session -> session.getStatus() == SessionStatus.ACTIVE
                        && (session.getAbsExpiresAt() == null || !Instant.now().isAfter(session.getAbsExpiresAt()))
                        && (session.getIdleExpiresAt() == null || !Instant.now().isAfter(session.getIdleExpiresAt())))
                .orElse(false);
        servletSessionRegistry.cacheActive(sessionId, active);
        return active;
    }

    @Transactional
    public void revokeOthers(UUID userId, UUID keepSessionId, String reason, UUID revokedBy) {
        for (IamSession session : sessionRepository.findByUserIdOrderByLastSeenAtDesc(userId)) {
            if (keepSessionId != null && keepSessionId.equals(session.getId())) {
                continue;
            }
            if (session.getStatus() == SessionStatus.ACTIVE) {
                revoke(session.getId(), reason, revokedBy);
            }
        }
    }

    @Transactional
    public void revoke(UUID sessionId, String reason) {
        revoke(sessionId, reason, null);
    }

    @Transactional
    public void revoke(UUID sessionId, String reason, UUID revokedBy) {
        if (sessionId == null) {
            return;
        }
        IamSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null || session.getStatus() == SessionStatus.REVOKED) {
            return;
        }
        Instant now = Instant.now();
        session.setStatus(SessionStatus.REVOKED);
        session.setRevokedAt(now);
        session.setRevokedBy(revokedBy);
        session.setRevokeReason(reason);
        sessionRepository.save(session);
        servletSessionRegistry.markRevoked(sessionId);
        for (IamRefreshToken token : refreshTokenRepository.findBySessionId(sessionId)) {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(now);
                refreshTokenRepository.save(token);
            }
        }
        for (IamOAuthAuthorization authorization : authorizationRepository.findBySessionId(sessionId)) {
            invalidate(authorization);
            authorizationRepository.save(authorization);
        }
        IamRevocation revocation = new IamRevocation();
        revocation.setSubjectType("session");
        revocation.setSubjectValue(sessionId.toString());
        revocation.setReason(reason);
        revocation.setRevokedAt(now);
        revocationRepository.save(revocation);
    }

    @Transactional
    public void revokeFamily(UUID familyId, String reason) {
        if (familyId == null) {
            return;
        }
        Instant now = Instant.now();
        UUID sessionId = null;
        for (IamRefreshToken token : refreshTokenRepository.findByFamilyId(familyId)) {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(now);
                refreshTokenRepository.save(token);
            }
            if (sessionId == null) {
                sessionId = token.getSessionId();
            }
        }
        IamRevocation revocation = new IamRevocation();
        revocation.setSubjectType("refresh_family");
        revocation.setSubjectValue(familyId.toString());
        revocation.setReason(reason);
        revocation.setRevokedAt(now);
        revocationRepository.save(revocation);
        if (sessionId != null) {
            revoke(sessionId, reason);
        }
    }

    private void invalidate(IamOAuthAuthorization authorization) {
        Map<String, Object> accessMeta = authorization.getAccessTokenMetadata() == null
                ? new HashMap<>()
                : new HashMap<>(authorization.getAccessTokenMetadata());
        accessMeta.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, true);
        authorization.setAccessTokenMetadata(accessMeta);
        if (authorization.getIdTokenHash() != null || authorization.getIdTokenMetadata() != null) {
            Map<String, Object> idMeta = authorization.getIdTokenMetadata() == null
                    ? new HashMap<>()
                    : new HashMap<>(authorization.getIdTokenMetadata());
            idMeta.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, true);
            authorization.setIdTokenMetadata(idMeta);
        }
    }

    private String resolveIp(HttpServletRequest request, Authentication authentication) {
        if (authentication.getDetails() instanceof WebAuthenticationDetails details
                && details.getRemoteAddress() != null) {
            return trim(details.getRemoteAddress(), 64);
        }
        return trim(request.getRemoteAddr(), 64);
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
