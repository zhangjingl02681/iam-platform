package com.enterprise.iam.session.web;

import com.enterprise.iam.auth.IamPrincipal;
import com.enterprise.iam.common.exception.NotFoundException;
import com.enterprise.iam.session.IamLoginSessionService;
import com.enterprise.iam.session.domain.IamSession;
import com.enterprise.iam.session.web.dto.SessionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/sessions")
@PreAuthorize("isAuthenticated()")
public class MeSessionController {

    private final IamLoginSessionService loginSessionService;

    public MeSessionController(IamLoginSessionService loginSessionService) {
        this.loginSessionService = loginSessionService;
    }

    @GetMapping
    public List<SessionResponse> list(@AuthenticationPrincipal IamPrincipal principal, HttpServletRequest request) {
        UUID currentId = loginSessionService.currentSessionId(request).orElse(null);
        String username = principal.getUsername();
        return loginSessionService.listForUser(principal.getUserId()).stream()
                .map(session -> SessionResponse.from(session, username, session.getId().equals(currentId)))
                .toList();
    }

    @PostMapping("/revoke-others")
    public ResponseEntity<Void> revokeOthers(
            @AuthenticationPrincipal IamPrincipal principal, HttpServletRequest request) {
        UUID currentId = loginSessionService
                .currentSessionId(request)
                .orElse(null);
        loginSessionService.revokeOthers(principal.getUserId(), currentId, "user_revoke_others", principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(
            @AuthenticationPrincipal IamPrincipal principal,
            @PathVariable UUID id,
            HttpServletRequest request) {
        IamSession session = loginSessionService.listForUser(principal.getUserId()).stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("session_not_found", "Session not found"));
        loginSessionService.revoke(session.getId(), "user_revoke", principal.getUserId());
        UUID currentId = loginSessionService.currentSessionId(request).orElse(null);
        if (session.getId().equals(currentId)) {
            HttpSession httpSession = request.getSession(false);
            if (httpSession != null) {
                httpSession.invalidate();
            }
        }
        return ResponseEntity.noContent().build();
    }
}
