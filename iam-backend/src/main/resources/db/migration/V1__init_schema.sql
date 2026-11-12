-- IAM platform initial schema (PostgreSQL 16).
-- Conventions: UUID primary keys, timestamptz, snake_case, append-only audit.

-- ---------------------------------------------------------------------------
-- Identity
-- ---------------------------------------------------------------------------

CREATE TABLE iam_org (
    id              UUID PRIMARY KEY,
    parent_id       UUID REFERENCES iam_org (id),
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    path            TEXT         NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_iam_org_code UNIQUE (code),
    CONSTRAINT chk_iam_org_status CHECK (status IN ('active', 'disabled'))
);

CREATE INDEX idx_iam_org_parent ON iam_org (parent_id);
CREATE INDEX idx_iam_org_path ON iam_org (path);
CREATE INDEX idx_iam_org_status ON iam_org (status);

CREATE TABLE iam_user (
    id              UUID PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL,
    employee_no     VARCHAR(64),
    display_name    VARCHAR(128) NOT NULL,
    email           VARCHAR(256),
    mobile          VARCHAR(32),
    status          VARCHAR(16)  NOT NULL,
    primary_org_id  UUID         REFERENCES iam_org (id),
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      UUID,
    updated_by      UUID,
    CONSTRAINT uq_iam_user_username UNIQUE (username),
    CONSTRAINT uq_iam_user_employee_no UNIQUE (employee_no),
    CONSTRAINT chk_iam_user_status CHECK (status IN ('active', 'disabled', 'locked', 'terminated'))
);

CREATE INDEX idx_iam_user_status ON iam_user (status);
CREATE INDEX idx_iam_user_primary_org ON iam_user (primary_org_id);
CREATE INDEX idx_iam_user_email ON iam_user (email);

CREATE TABLE iam_credential (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES iam_user (id),
    type            VARCHAR(32)  NOT NULL,
    secret_hash     VARCHAR(255) NOT NULL,
    changed_at      TIMESTAMPTZ  NOT NULL,
    must_change     BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_count    INTEGER      NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_iam_credential_type CHECK (type IN ('password')),
    CONSTRAINT uq_iam_credential_user_type UNIQUE (user_id, type)
);

CREATE TABLE iam_password_history (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES iam_user (id),
    secret_hash     VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_iam_password_history_user ON iam_password_history (user_id, created_at DESC);

CREATE TABLE iam_mfa_factor (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES iam_user (id),
    type            VARCHAR(32)  NOT NULL,
    secret_enc      TEXT,
    label           VARCHAR(128),
    verified_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    disabled_at     TIMESTAMPTZ
);

CREATE INDEX idx_iam_mfa_factor_user ON iam_mfa_factor (user_id);

CREATE TABLE iam_attribute_def (
    id              UUID PRIMARY KEY,
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    value_type      VARCHAR(32)  NOT NULL,
    multi_valued    BOOLEAN      NOT NULL DEFAULT FALSE,
    required        BOOLEAN      NOT NULL DEFAULT FALSE,
    description     TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_iam_attribute_def_code UNIQUE (code)
);

CREATE TABLE iam_user_attribute (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES iam_user (id),
    attr_code       VARCHAR(64)  NOT NULL,
    value_text      TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_iam_user_attribute UNIQUE (user_id, attr_code)
);

CREATE INDEX idx_iam_user_attribute_user ON iam_user_attribute (user_id, attr_code);

CREATE TABLE iam_position (
    id              UUID PRIMARY KEY,
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_iam_position_code UNIQUE (code),
    CONSTRAINT chk_iam_position_status CHECK (status IN ('active', 'disabled'))
);

CREATE TABLE iam_user_assignment (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES iam_user (id),
    org_id          UUID         NOT NULL REFERENCES iam_org (id),
    position_id     UUID         REFERENCES iam_position (id),
    is_primary      BOOLEAN      NOT NULL DEFAULT FALSE,
    valid_from      TIMESTAMPTZ,
    valid_to        TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_iam_user_assignment_user ON iam_user_assignment (user_id);
CREATE INDEX idx_iam_user_assignment_org ON iam_user_assignment (org_id);
CREATE UNIQUE INDEX uq_iam_user_one_primary_assignment
    ON iam_user_assignment (user_id) WHERE is_primary;

CREATE TABLE iam_role (
    id              UUID PRIMARY KEY,
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    description     TEXT,
    status          VARCHAR(16)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_iam_role_code UNIQUE (code),
    CONSTRAINT chk_iam_role_status CHECK (status IN ('active', 'disabled'))
);

CREATE TABLE iam_user_role (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES iam_user (id),
    role_id         UUID         NOT NULL REFERENCES iam_role (id),
    scope_org_id    UUID         REFERENCES iam_org (id),
    valid_from      TIMESTAMPTZ,
    valid_to        TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_iam_user_role_user ON iam_user_role (user_id);
CREATE INDEX idx_iam_user_role_role ON iam_user_role (role_id);

CREATE TABLE iam_import_job (
    id              UUID PRIMARY KEY,
    type            VARCHAR(32)  NOT NULL,
    file_ref        TEXT,
    status          VARCHAR(16)  NOT NULL,
    total           INTEGER,
    success_count   INTEGER,
    failed_count    INTEGER,
    error_report    TEXT,
    created_by      UUID,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at     TIMESTAMPTZ,
    CONSTRAINT chk_iam_import_job_status
        CHECK (status IN ('pending', 'running', 'succeeded', 'failed', 'cancelled'))
);

CREATE INDEX idx_iam_import_job_created ON iam_import_job (created_at DESC);

-- ---------------------------------------------------------------------------
-- Applications / OAuth clients (storage only in this drop; no protocol crypto)
-- ---------------------------------------------------------------------------

CREATE TABLE iam_application (
    id              UUID PRIMARY KEY,
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    owner_org_id    UUID         REFERENCES iam_org (id),
    description     TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_iam_application_code UNIQUE (code),
    CONSTRAINT chk_iam_application_status CHECK (status IN ('active', 'disabled'))
);

CREATE TABLE iam_oauth_client (
    id                      UUID PRIMARY KEY,
    client_id               VARCHAR(128) NOT NULL,
    app_id                  UUID         NOT NULL REFERENCES iam_application (id),
    client_type             VARCHAR(32)  NOT NULL,
    grant_types             TEXT,
    redirect_uris           TEXT,
    scopes                  TEXT,
    pkce_required           BOOLEAN      NOT NULL DEFAULT TRUE,
    token_endpoint_auth     VARCHAR(64),
    client_secret_hash      VARCHAR(255),
    status                  VARCHAR(16)  NOT NULL,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_iam_oauth_client_client_id UNIQUE (client_id),
    CONSTRAINT chk_iam_oauth_client_type CHECK (client_type IN ('public', 'confidential')),
    CONSTRAINT chk_iam_oauth_client_status CHECK (status IN ('active', 'disabled'))
);

CREATE INDEX idx_iam_oauth_client_app ON iam_oauth_client (app_id);

CREATE TABLE iam_scope (
    code            VARCHAR(64) PRIMARY KEY,
    description     TEXT
);

-- ---------------------------------------------------------------------------
-- Policy (ABAC documents; PDP evaluation is later)
-- ---------------------------------------------------------------------------

CREATE TABLE iam_policy (
    id              UUID PRIMARY KEY,
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    description     TEXT,
    created_by      UUID,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_iam_policy_code UNIQUE (code),
    CONSTRAINT chk_iam_policy_status CHECK (status IN ('draft', 'published', 'deprecated'))
);

CREATE TABLE iam_policy_version (
    id              UUID PRIMARY KEY,
    policy_id       UUID         NOT NULL REFERENCES iam_policy (id),
    version         INTEGER      NOT NULL,
    language        VARCHAR(32),
    document        TEXT         NOT NULL,
    checksum        VARCHAR(128),
    status          VARCHAR(16)  NOT NULL,
    published_at    TIMESTAMPTZ,
    published_by    UUID,
    comment         TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_iam_policy_version UNIQUE (policy_id, version),
    CONSTRAINT chk_iam_policy_version_status CHECK (status IN ('draft', 'published', 'deprecated'))
);

CREATE TABLE iam_policy_binding (
    id              UUID PRIMARY KEY,
    policy_id       UUID         NOT NULL REFERENCES iam_policy (id),
    version_id      UUID         NOT NULL REFERENCES iam_policy_version (id),
    target_type     VARCHAR(32)  NOT NULL,
    target_id       UUID         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_iam_policy_binding_policy ON iam_policy_binding (policy_id);
CREATE INDEX idx_iam_policy_binding_target ON iam_policy_binding (target_type, target_id);

-- ---------------------------------------------------------------------------
-- Sessions / tokens (persisted in Postgres; Redis will cache hot sessions later)
-- Secrets are stored as hashes only. Signing keys do not belong here.
-- ---------------------------------------------------------------------------

CREATE TABLE iam_session (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES iam_user (id),
    amr             VARCHAR(128),
    ip              VARCHAR(64),
    user_agent      VARCHAR(512),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idle_expires_at TIMESTAMPTZ  NOT NULL,
    abs_expires_at  TIMESTAMPTZ  NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    revoked_at      TIMESTAMPTZ,
    revoked_by      UUID,
    revoke_reason   VARCHAR(256),
    CONSTRAINT chk_iam_session_status CHECK (status IN ('active', 'revoked', 'expired'))
);

CREATE INDEX idx_iam_session_user_status ON iam_session (user_id, status);
CREATE INDEX idx_iam_session_expires ON iam_session (idle_expires_at, abs_expires_at);

CREATE TABLE iam_authorization_code (
    id                  UUID PRIMARY KEY,
    code_hash           VARCHAR(128) NOT NULL,
    client_id           VARCHAR(128) NOT NULL,
    user_id             UUID         NOT NULL REFERENCES iam_user (id),
    session_id          UUID         REFERENCES iam_session (id),
    redirect_uri        TEXT         NOT NULL,
    code_challenge      VARCHAR(256),
    challenge_method    VARCHAR(16),
    scope               TEXT,
    expires_at          TIMESTAMPTZ  NOT NULL,
    consumed_at         TIMESTAMPTZ,
    nonce               VARCHAR(256),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_iam_authorization_code_hash UNIQUE (code_hash)
);

CREATE INDEX idx_iam_authorization_code_expires ON iam_authorization_code (expires_at);

CREATE TABLE iam_refresh_token (
    id              UUID PRIMARY KEY,
    token_hash      VARCHAR(128) NOT NULL,
    family_id       UUID         NOT NULL,
    user_id         UUID         NOT NULL REFERENCES iam_user (id),
    client_id       VARCHAR(128) NOT NULL,
    session_id      UUID         REFERENCES iam_session (id),
    scope           TEXT,
    expires_at      TIMESTAMPTZ  NOT NULL,
    revoked_at      TIMESTAMPTZ,
    replaced_by     UUID,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_iam_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_iam_refresh_token_user ON iam_refresh_token (user_id, client_id);
CREATE INDEX idx_iam_refresh_token_family ON iam_refresh_token (family_id);

CREATE TABLE iam_revocation (
    id              UUID PRIMARY KEY,
    subject_type    VARCHAR(32)  NOT NULL,
    subject_value   VARCHAR(256) NOT NULL,
    reason          VARCHAR(256),
    revoked_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_by      UUID
);

CREATE INDEX idx_iam_revocation_subject ON iam_revocation (subject_type, subject_value);

-- ---------------------------------------------------------------------------
-- Audit (append-only). Application code must never update or delete rows.
-- Monthly RANGE partitioning on occurred_at is deferred: a partitioned table
-- would require (id, occurred_at) as the primary key, which we do not want
-- to introduce in V1. Revisit when volume requires it.
-- ---------------------------------------------------------------------------

CREATE TABLE iam_audit_event (
    id              UUID PRIMARY KEY,
    occurred_at     TIMESTAMPTZ  NOT NULL,
    event_type      VARCHAR(64)  NOT NULL,
    actor_type      VARCHAR(32)  NOT NULL,
    actor_id        UUID,
    object_type     VARCHAR(64),
    object_id       UUID,
    action          VARCHAR(32)  NOT NULL,
    result          VARCHAR(16)  NOT NULL,
    ip              VARCHAR(64),
    user_agent      VARCHAR(512),
    request_id      VARCHAR(64),
    session_id      UUID,
    client_id       VARCHAR(128),
    detail          JSONB,
    prev_hash       VARCHAR(64),
    event_hash      VARCHAR(64)  NOT NULL
);

CREATE INDEX idx_iam_audit_occurred_at ON iam_audit_event (occurred_at);
CREATE INDEX idx_iam_audit_actor_occurred ON iam_audit_event (actor_id, occurred_at);
CREATE INDEX idx_iam_audit_event_type_occurred ON iam_audit_event (event_type, occurred_at);
CREATE INDEX idx_iam_audit_object_occurred ON iam_audit_event (object_type, object_id, occurred_at);

COMMENT ON TABLE iam_audit_event IS
    'Append-only audit log. Do not GRANT UPDATE/DELETE to the application role. '
    'Future optimization: RANGE partition by occurred_at (monthly).';

-- ---------------------------------------------------------------------------
-- PAM reserved tables (phase 2). Credentials live in a vault; only vault_ref
-- is stored here. No signing keys or plaintext secrets.
-- ---------------------------------------------------------------------------

CREATE TABLE pam_target (
    id              UUID PRIMARY KEY,
    type            VARCHAR(32)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    address         VARCHAR(256),
    env             VARCHAR(32),
    status          VARCHAR(16)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pam_target_status CHECK (status IN ('active', 'disabled'))
);

CREATE TABLE pam_account (
    id              UUID PRIMARY KEY,
    target_id       UUID         NOT NULL REFERENCES pam_target (id),
    username        VARCHAR(128) NOT NULL,
    vault_ref       VARCHAR(256),
    rotate_cron     VARCHAR(64),
    status          VARCHAR(16)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pam_account_status CHECK (status IN ('active', 'disabled', 'rotating'))
);

CREATE INDEX idx_pam_account_target ON pam_account (target_id);

CREATE TABLE pam_request (
    id              UUID PRIMARY KEY,
    account_id      UUID         NOT NULL REFERENCES pam_account (id),
    requester_id    UUID         NOT NULL REFERENCES iam_user (id),
    reason          TEXT,
    start_at        TIMESTAMPTZ  NOT NULL,
    end_at          TIMESTAMPTZ  NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    approver_id     UUID         REFERENCES iam_user (id),
    decided_at      TIMESTAMPTZ,
    break_glass     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pam_request_status
        CHECK (status IN ('pending', 'approved', 'rejected', 'cancelled', 'expired'))
);

CREATE INDEX idx_pam_request_account ON pam_request (account_id);
CREATE INDEX idx_pam_request_requester ON pam_request (requester_id);

CREATE TABLE pam_checkout (
    id              UUID PRIMARY KEY,
    request_id      UUID         NOT NULL REFERENCES pam_request (id),
    account_id      UUID         NOT NULL REFERENCES pam_account (id),
    user_id         UUID         NOT NULL REFERENCES iam_user (id),
    checked_out_at  TIMESTAMPTZ  NOT NULL,
    expires_at      TIMESTAMPTZ  NOT NULL,
    checked_in_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pam_checkout_account ON pam_checkout (account_id, user_id);

CREATE TABLE pam_session_record (
    id              UUID PRIMARY KEY,
    checkout_id     UUID         NOT NULL REFERENCES pam_checkout (id),
    started_at      TIMESTAMPTZ  NOT NULL,
    ended_at        TIMESTAMPTZ,
    storage_uri     TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
