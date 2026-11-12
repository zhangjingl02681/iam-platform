-- OIDC / OAuth 2.1 persistence used by Spring Authorization Server adapters.
-- Authorization codes and refresh tokens remain hash-only in the original tables.

ALTER TABLE iam_oauth_client
    ADD COLUMN post_logout_redirect_uris TEXT,
    ADD COLUMN require_consent BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE iam_authorization_code
    ADD COLUMN authorization_id UUID;

ALTER TABLE iam_refresh_token
    ADD COLUMN authorization_id UUID;

CREATE INDEX idx_iam_authorization_code_authorization
    ON iam_authorization_code (authorization_id);
CREATE INDEX idx_iam_refresh_token_authorization
    ON iam_refresh_token (authorization_id);

CREATE TABLE iam_oauth_authorization (
    id                          UUID PRIMARY KEY,
    registered_client_id        VARCHAR(64)  NOT NULL,
    principal_name              VARCHAR(256) NOT NULL,
    user_id                     UUID         REFERENCES iam_user (id),
    session_id                  UUID         REFERENCES iam_session (id),
    grant_type                  VARCHAR(64)  NOT NULL,
    authorized_scopes           TEXT,
    state                       VARCHAR(256),
    authorization_request_json  JSONB,
    access_token_hash           VARCHAR(128),
    access_token_issued_at      TIMESTAMPTZ,
    access_token_expires_at     TIMESTAMPTZ,
    access_token_scopes         TEXT,
    access_token_type           VARCHAR(32),
    access_token_metadata_json  JSONB,
    id_token_hash               VARCHAR(128),
    id_token_issued_at          TIMESTAMPTZ,
    id_token_expires_at         TIMESTAMPTZ,
    id_token_claims_json        JSONB,
    id_token_metadata_json      JSONB,
    refresh_family_id           UUID,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_iam_oauth_authorization_access_hash
    ON iam_oauth_authorization (access_token_hash);
CREATE INDEX idx_iam_oauth_authorization_session
    ON iam_oauth_authorization (session_id);
CREATE INDEX idx_iam_oauth_authorization_client
    ON iam_oauth_authorization (registered_client_id);

COMMENT ON TABLE iam_oauth_authorization IS
    'Authorization Server grant state. Raw authorization codes, refresh tokens, and client secrets are never stored here; only hashes go to iam_authorization_code / iam_refresh_token / iam_oauth_client.';
