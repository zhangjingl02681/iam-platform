package com.enterprise.iam.session.web;

import com.enterprise.iam.auth.IamPrincipal;
import com.enterprise.iam.common.exception.NotFoundException;
import com.enterprise.iam.common.web.PageResponse;
import com.enterprise.iam.identity.domain.IamUser;
import com.enterprise.iam.identity.repo.IamUserRepository;
import com.enterprise.iam.identity.service.UserService;
import com.enterprise.iam.session.IamLoginSessionService;
import com.enterprise.iam.session.domain.IamSession;
import com.enterprise.iam.session.domain.SessionStatus;
import com.enterprise.iam.session.repo.IamSessionRepository;
import com.enterprise.iam.session.web.dto.SessionResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSessionController {

    private final IamLoginSessionService loginSessionService;
    private final IamSessionRepository sessionRepository;
    private final IamUserRepository userRepository;

    public AdminSessionController(
            IamLoginSessionService loginSessionService,
            IamSessionRepository sessionRepository,
            IamUserRepository userRepository) {
        this.loginSessionService = loginSessionService;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public PageResponse<SessionResponse> search(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) SessionStatus status,
            @PageableDefault(size = 20, sort = "lastSeenAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {
        UUID resolvedUserId = userId;
        if (resolvedUserId == null && StringUtils.hasText(username)) {
            resolvedUserId = userRepository
                    .findByUsername(UserService.normalizeUsername(username))
                    .map(IamUser::getId)
                    .orElse(null);
            if (resolvedUserId == null) {
                return new PageResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0);
            }
        }
        Page<IamSession> page = loginSessionService.search(resolvedUserId, status, pageable);
        Map<UUID, String> usernames = userRepository
                .findAllById(page.getContent().stream().map(IamSession::getUserId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(IamUser::getId, IamUser::getUsername));
        UUID currentId = loginSessionService.currentSessionId(request).orElse(null);
        return PageResponse.from(page.map(session -> SessionResponse.from(
                session,
                usernames.get(session.getUserId()),
                session.getId().equals(currentId))));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<Void> revoke(
            @PathVariable UUID id,
            @AuthenticationPrincipal IamPrincipal principal,
            HttpServletRequest request) {
        IamSession session = sessionRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("session_not_found", "Session not found"));
        loginSessionService.revoke(session.getId(), "admin_revoke", principal.getUserId());
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
