package com.enterprise.iam.session;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.event.EventListener;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.stereotype.Component;

/**
 * In-process registry that binds IAM sessions to servlet sessions so revocation
 * can immediately invalidate the HTTP session. Redis remains the production
 * scale-out option and is not required in this increment.
 */
@Component
public class IamServletSessionRegistry {

    private final Map<UUID, HttpSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> activeCache = new ConcurrentHashMap<>();

    public void bind(UUID iamSessionId, HttpSession httpSession) {
        if (iamSessionId == null || httpSession == null) {
            return;
        }
        sessions.put(iamSessionId, httpSession);
        activeCache.put(iamSessionId, Boolean.TRUE);
        try {
            httpSession.setAttribute(IamLoginSessionService.SESSION_ATTRIBUTE, iamSessionId);
        } catch (IllegalStateException ignored) {
            sessions.remove(iamSessionId);
        }
    }

    public void unbind(UUID iamSessionId) {
        if (iamSessionId != null) {
            sessions.remove(iamSessionId);
            activeCache.remove(iamSessionId);
        }
    }

    public void markRevoked(UUID iamSessionId) {
        if (iamSessionId == null) {
            return;
        }
        activeCache.put(iamSessionId, Boolean.FALSE);
        HttpSession httpSession = sessions.remove(iamSessionId);
        if (httpSession != null) {
            try {
                httpSession.invalidate();
            } catch (IllegalStateException ignored) {
                // already invalidated
            }
        }
    }

    public Boolean cachedActive(UUID iamSessionId) {
        return iamSessionId == null ? null : activeCache.get(iamSessionId);
    }

    public void cacheActive(UUID iamSessionId, boolean active) {
        if (iamSessionId != null) {
            activeCache.put(iamSessionId, active);
        }
    }

    @EventListener
    public void onSessionDestroyed(HttpSessionDestroyedEvent event) {
        HttpSession httpSession = event.getSession();
        Object value;
        try {
            value = httpSession.getAttribute(IamLoginSessionService.SESSION_ATTRIBUTE);
        } catch (IllegalStateException ex) {
            return;
        }
        UUID id = toId(value);
        if (id != null) {
            sessions.remove(id, httpSession);
        }
    }

    private static UUID toId(Object value) {
        if (value instanceof UUID id) {
            return id;
        }
        if (value instanceof String text) {
            try {
                return UUID.fromString(text);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }
}
