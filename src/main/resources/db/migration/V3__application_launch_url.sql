-- Launch metadata for the SSO portal application catalog.
ALTER TABLE iam_application
    ADD COLUMN launch_url VARCHAR(1024),
    ADD COLUMN icon VARCHAR(64),
    ADD COLUMN accent_color VARCHAR(16);

COMMENT ON COLUMN iam_application.launch_url IS
    'Optional browser launch URL for the SSO portal. Relative paths and http(s) URLs are allowed.';
