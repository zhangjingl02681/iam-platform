-- MFA challenges, hashed recovery codes, and signing-key metadata.
-- Private signing material and PAM/MFA secrets stay out of these tables (vault_ref only).

CREATE TABLE iam_mfa_recovery_code (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES iam_user (id),
    factor_id       UUID         NOT NULL REFERENCES iam_mfa_factor (id),
    code_hash       TEXT         NOT NULL,
    used_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_iam_mfa_recovery_code_user ON iam_mfa_recovery_code (user_id);
CREATE INDEX idx_iam_mfa_recovery_code_factor ON iam_mfa_recovery_code (factor_id);

CREATE TABLE iam_mfa_challenge (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES iam_user (id),
    token_hash      TEXT         NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ  NOT NULL,
    consumed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_iam_mfa_challenge_user ON iam_mfa_challenge (user_id);
CREATE INDEX idx_iam_mfa_challenge_expires ON iam_mfa_challenge (expires_at);

CREATE TABLE iam_signing_key (
    id              UUID PRIMARY KEY,
    kid             VARCHAR(128) NOT NULL UNIQUE,
    vault_ref       VARCHAR(256) NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    valid_until     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retired_at      TIMESTAMPTZ,
    CONSTRAINT chk_iam_signing_key_status CHECK (status IN ('current', 'previous', 'retired'))
);

CREATE INDEX idx_iam_signing_key_status ON iam_signing_key (status);
