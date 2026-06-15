ALTER TABLE registrations
    ADD COLUMN admin_memo TEXT,
    ADD COLUMN newcomer BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN care_target BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE registration_histories
    DROP CONSTRAINT ck_registration_histories_change_type;

ALTER TABLE registration_histories
    ADD CONSTRAINT ck_registration_histories_change_type CHECK (
        change_type IN (
            'CREATED',
            'OVERWRITTEN',
            'SELF_UPDATED',
            'KEY_REISSUED',
            'FEE_PAYMENT_UPDATED',
            'STATUS_UPDATED',
            'ADMIN_MANAGEMENT_UPDATED'
        )
    );

CREATE TABLE registration_privacy_access_logs (
    id BIGSERIAL PRIMARY KEY,
    registration_id BIGINT NOT NULL REFERENCES registrations (id),
    admin_user_id BIGINT NOT NULL REFERENCES admin_users (id),
    access_type VARCHAR(50) NOT NULL,
    sensitive_fields VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_registration_privacy_access_logs_access_type CHECK (
        access_type IN ('DETAIL_VIEW', 'HISTORY_VIEW')
    )
);

CREATE INDEX idx_registration_privacy_access_logs_registration_created_at
ON registration_privacy_access_logs (registration_id, created_at DESC);

CREATE INDEX idx_registration_privacy_access_logs_admin_created_at
ON registration_privacy_access_logs (admin_user_id, created_at DESC);
