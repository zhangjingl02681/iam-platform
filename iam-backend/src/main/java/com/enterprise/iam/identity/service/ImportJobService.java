package com.enterprise.iam.identity.service;

import com.enterprise.iam.audit.AuditService;
import com.enterprise.iam.common.SecurityUtils;
import com.enterprise.iam.common.exception.BusinessException;
import com.enterprise.iam.common.exception.NotFoundException;
import com.enterprise.iam.identity.domain.IamImportJob;
import com.enterprise.iam.identity.domain.ImportJobStatus;
import com.enterprise.iam.identity.domain.UserStatus;
import com.enterprise.iam.identity.repo.IamImportJobRepository;
import com.enterprise.iam.identity.web.dto.CreateImportJobRequest;
import com.enterprise.iam.identity.web.dto.ImportJobResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class ImportJobService {

    public static final String USER_CSV = "user_csv";

    private final IamImportJobRepository repository;
    private final AuditService auditService;
    private final UserService userService;
    private final TransactionTemplate requiresNew;

    public ImportJobService(
            IamImportJobRepository repository,
            AuditService auditService,
            UserService userService,
            PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.auditService = auditService;
        this.userService = userService;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public ImportJobResponse create(CreateImportJobRequest request) {
        if (StringUtils.hasText(request.csv())) {
            return createAndParse(request.type(), "inline:csv", request.csv());
        }
        IamImportJob job = newPending(request.type(), request.fileRef());
        job = repository.save(job);
        auditService.record("import_job.create", "create", "import_job", job.getId(), "success",
                Map.of("type", job.getType()));
        return ImportJobResponse.from(job);
    }

    public ImportJobResponse createAndParse(String type, String fileRef, String csv) {
        IamImportJob job = newPending(type, fileRef);
        job = repository.saveAndFlush(job);
        auditService.record("import_job.create", "create", "import_job", job.getId(), "success",
                Map.of("type", job.getType(), "parsed", true));
        return execute(job, csv);
    }

    private ImportJobResponse execute(IamImportJob job, String csv) {
        job.setStatus(ImportJobStatus.RUNNING);
        job = repository.saveAndFlush(job);
        try {
            List<CsvRow> rows = parseUserCsv(csv);
            int success = 0;
            int failed = 0;
            StringBuilder errors = new StringBuilder();
            for (CsvRow row : rows) {
                try {
                    CsvRow current = row;
                    requiresNew.executeWithoutResult(status -> userService.importRow(
                            current.username(),
                            current.displayName(),
                            current.email(),
                            current.orgCode(),
                            current.status(),
                            current.temporaryPassword()));
                    success++;
                } catch (RuntimeException ex) {
                    failed++;
                    appendError(errors, row.lineNumber(), safeMessage(ex));
                }
            }
            job.setTotal(rows.size());
            job.setSuccessCount(success);
            job.setFailedCount(failed);
            job.setErrorReport(errors.isEmpty() ? null : errors.toString().trim());
            job.setStatus(success == 0 ? ImportJobStatus.FAILED : ImportJobStatus.SUCCEEDED);
            job.setFinishedAt(Instant.now());
            job = repository.save(job);
            auditService.record(
                    "import_job.complete",
                    "update",
                    "import_job",
                    job.getId(),
                    job.getStatus() == ImportJobStatus.SUCCEEDED ? "success" : "failure",
                    Map.of("successCount", success, "failedCount", failed));
            return ImportJobResponse.from(job);
        } catch (RuntimeException ex) {
            job.setStatus(ImportJobStatus.FAILED);
            job.setTotal(0);
            job.setSuccessCount(0);
            job.setFailedCount(0);
            job.setErrorReport(safeMessage(ex));
            job.setFinishedAt(Instant.now());
            job = repository.save(job);
            auditService.record("import_job.complete", "update", "import_job", job.getId(), "failure",
                    Map.of("reason", "parse_error"));
            return ImportJobResponse.from(job);
        }
    }

    @Transactional(readOnly = true)
    public ImportJobResponse get(UUID id) {
        return ImportJobResponse.from(repository.findById(id)
                .orElseThrow(() -> new NotFoundException("import_job_not_found", "Import job not found")));
    }

    @Transactional(readOnly = true)
    public Page<ImportJobResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(ImportJobResponse::from);
    }

    private IamImportJob newPending(String type, String fileRef) {
        if (!StringUtils.hasText(type)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid_import_type", "Import type is required");
        }
        IamImportJob job = new IamImportJob();
        job.setType(type.trim());
        job.setFileRef(blankToNull(fileRef));
        job.setStatus(ImportJobStatus.PENDING);
        job.setTotal(0);
        job.setSuccessCount(0);
        job.setFailedCount(0);
        job.setCreatedBy(SecurityUtils.currentUserId().orElse(null));
        return job;
    }

    static List<CsvRow> parseUserCsv(String csv) {
        if (!StringUtils.hasText(csv)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid_csv", "CSV content is required");
        }
        List<List<String>> table = parseCsv(csv);
        if (table.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid_csv", "CSV content is empty");
        }
        List<String> header = table.get(0).stream().map(ImportJobService::normalizeHeader).toList();
        int usernameIdx = indexOf(header, "username");
        int displayNameIdx = indexOf(header, "display_name", "displayname");
        if (usernameIdx < 0 || displayNameIdx < 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_csv",
                    "CSV must include username and display_name columns");
        }
        int emailIdx = indexOf(header, "email");
        int orgIdx = indexOf(header, "org_code", "orgcode");
        int statusIdx = indexOf(header, "status");
        int passwordIdx = indexOf(header, "temporary_password", "temporarypassword");
        List<CsvRow> rows = new ArrayList<>();
        for (int i = 1; i < table.size(); i++) {
            List<String> cells = table.get(i);
            if (cells.stream().allMatch(value -> !StringUtils.hasText(value))) {
                continue;
            }
            rows.add(new CsvRow(
                    i + 1,
                    cell(cells, usernameIdx),
                    cell(cells, displayNameIdx),
                    cell(cells, emailIdx),
                    cell(cells, orgIdx),
                    parseStatus(cell(cells, statusIdx), i + 1),
                    cell(cells, passwordIdx)));
        }
        return rows;
    }

    private static UserStatus parseStatus(String raw, int lineNumber) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String value = raw.trim();
        for (UserStatus status : UserStatus.values()) {
            if (status.name().equalsIgnoreCase(value) || status.code().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new BusinessException(
                HttpStatus.BAD_REQUEST, "invalid_status", "row " + lineNumber + ": unknown status " + raw.trim());
    }

    private static String cell(List<String> cells, int index) {
        if (index < 0 || index >= cells.size()) {
            return null;
        }
        String value = cells.get(index);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static int indexOf(List<String> header, String... names) {
        for (int i = 0; i < header.size(); i++) {
            String current = header.get(i);
            for (String name : names) {
                if (name.equals(current)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String normalizeHeader(String header) {
        return header == null ? "" : header.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    static List<List<String>> parseCsv(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < csv.length(); i++) {
            char ch = csv.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cell.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                current.add(cell.toString());
                cell.setLength(0);
            } else if (ch == '\n') {
                current.add(cell.toString());
                cell.setLength(0);
                rows.add(current);
                current = new ArrayList<>();
            } else if (ch != '\r') {
                cell.append(ch);
            }
        }
        if (inQuotes) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid_csv", "Unclosed quoted field in CSV");
        }
        if (cell.length() > 0 || !current.isEmpty()) {
            current.add(cell.toString());
            rows.add(current);
        }
        return rows;
    }

    private static void appendError(StringBuilder errors, int line, String message) {
        if (!errors.isEmpty()) {
            errors.append('\n');
        }
        String cleaned = message == null ? "row failed" : message;
        if (!cleaned.startsWith("row ")) {
            cleaned = "row " + line + ": " + cleaned;
        }
        errors.append(cleaned);
    }

    private static String safeMessage(Throwable ex) {
        String message = ex.getMessage();
        if (!StringUtils.hasText(message)) {
            message = ex.getClass().getSimpleName();
        }
        return message.replaceAll("(?i)temporary_password\\s*[:=]\\s*\\S+", "temporary_password=***");
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    record CsvRow(
            int lineNumber,
            String username,
            String displayName,
            String email,
            String orgCode,
            UserStatus status,
            String temporaryPassword) {}
}
