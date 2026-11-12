-- Application entitlements: grants of an application to a user, organization, or role.
CREATE TABLE iam_application_entitlement (
    id              UUID PRIMARY KEY,
    application_id  UUID         NOT NULL REFERENCES iam_application (id),
    subject_type    VARCHAR(16)  NOT NULL,
    subject_id      UUID         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_iam_application_entitlement UNIQUE (application_id, subject_type, subject_id),
    CONSTRAINT chk_iam_application_entitlement_subject
        CHECK (subject_type IN ('user', 'org', 'role'))
);

CREATE INDEX idx_iam_application_entitlement_app ON iam_application_entitlement (application_id);
CREATE INDEX idx_iam_application_entitlement_subject
    ON iam_application_entitlement (subject_type, subject_id);
