package com.enterprise.iam.application.service;

import com.enterprise.iam.application.domain.ApplicationStatus;
import com.enterprise.iam.application.domain.IamApplication;
import com.enterprise.iam.application.repo.IamApplicationRepository;
import com.enterprise.iam.application.web.dto.ApplicationResponse;
import com.enterprise.iam.application.web.dto.CreateApplicationRequest;
import com.enterprise.iam.application.web.dto.UpdateApplicationRequest;
import com.enterprise.iam.audit.AuditService;
import com.enterprise.iam.common.exception.BusinessException;
import com.enterprise.iam.common.exception.ConflictException;
import com.enterprise.iam.common.exception.NotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ApplicationService {

    private static final Pattern LAUNCH_URL =
            Pattern.compile("^(https?://.+|/.*)$", Pattern.CASE_INSENSITIVE);

    private final IamApplicationRepository applicationRepository;
    private final AuditService auditService;

    public ApplicationService(IamApplicationRepository applicationRepository, AuditService auditService) {
        this.applicationRepository = applicationRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ApplicationResponse create(CreateApplicationRequest request) {
        String code = normalizeCode(request.code());
        if (applicationRepository.existsByCode(code)) {
            throw new ConflictException("application_code_exists", "Application code already exists");
        }
        IamApplication application = new IamApplication();
        application.setCode(code);
        application.setName(request.name().trim());
        application.setDescription(blankToNull(request.description()));
        application.setLaunchUrl(normalizeLaunchUrl(request.launchUrl()));
        application.setIcon(blankToNull(request.icon()));
        application.setAccentColor(blankToNull(request.accentColor()));
        application.setOwnerOrgId(request.ownerOrgId());
        application.setStatus(request.status() == null ? ApplicationStatus.ACTIVE : request.status());
        application = applicationRepository.save(application);
        auditService.record(
                "application.create",
                "create",
                "application",
                application.getId(),
                "success",
                Map.of("code", application.getCode()));
        return ApplicationResponse.from(application);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse get(UUID id) {
        return ApplicationResponse.from(require(id));
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> search(String query, ApplicationStatus status, Pageable pageable) {
        Page<IamApplication> page;
        boolean hasQuery = StringUtils.hasText(query);
        if (hasQuery && status != null) {
            page = applicationRepository.searchByQueryAndStatus(query.trim(), status, pageable);
        } else if (hasQuery) {
            page = applicationRepository.searchByQuery(query.trim(), pageable);
        } else if (status != null) {
            page = applicationRepository.findByStatus(status, pageable);
        } else {
            page = applicationRepository.findAll(pageable);
        }
        return page.map(ApplicationResponse::from);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> listLaunchableForCurrentUser() {
        return applicationRepository
                .findByStatusAndLaunchUrlIsNotNullOrderByNameAsc(ApplicationStatus.ACTIVE)
                .stream()
                .map(ApplicationResponse::from)
                .toList();
    }

    @Transactional
    public ApplicationResponse update(UUID id, UpdateApplicationRequest request) {
        IamApplication application = require(id);
        if (request.name() != null) {
            if (!StringUtils.hasText(request.name())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "validation_error", "Name must not be blank");
            }
            application.setName(request.name().trim());
        }
        if (request.description() != null) {
            application.setDescription(blankToNull(request.description()));
        }
        if (request.launchUrl() != null) {
            application.setLaunchUrl(normalizeLaunchUrl(request.launchUrl()));
        }
        if (request.icon() != null) {
            application.setIcon(blankToNull(request.icon()));
        }
        if (request.accentColor() != null) {
            application.setAccentColor(blankToNull(request.accentColor()));
        }
        if (request.status() != null) {
            application.setStatus(request.status());
        }
        if (request.ownerOrgId() != null) {
            application.setOwnerOrgId(request.ownerOrgId());
        }
        application = applicationRepository.save(application);
        auditService.record(
                "application.update",
                "update",
                "application",
                application.getId(),
                "success",
                Map.of("code", application.getCode()));
        return ApplicationResponse.from(application);
    }

    private IamApplication require(UUID id) {
        return applicationRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("application_not_found", "Application not found"));
    }

    static String normalizeCode(String code) {
        return code == null ? "" : code.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeLaunchUrl(String launchUrl) {
        if (!StringUtils.hasText(launchUrl)) {
            return null;
        }
        String value = launchUrl.trim();
        if (!LAUNCH_URL.matcher(value).matches()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_launch_url",
                    "Launch URL must be an http(s) URL or a path starting with /");
        }
        return value;
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
