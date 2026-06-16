ALTER TABLE registrations
    ADD COLUMN fee_status_updated_at TIMESTAMPTZ,
    ADD COLUMN fee_status_updated_by_admin_id BIGINT REFERENCES admin_users (id);

CREATE TABLE registration_fee_events (
    id BIGSERIAL PRIMARY KEY,
    registration_id BIGINT NOT NULL REFERENCES registrations (id),
    previous_fee_paid BOOLEAN NOT NULL,
    new_fee_paid BOOLEAN NOT NULL,
    changed_by_admin_id BIGINT NOT NULL REFERENCES admin_users (id),
    reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_registration_fee_events_change CHECK (previous_fee_paid <> new_fee_paid),
    CONSTRAINT ck_registration_fee_events_unpaid_reason CHECK (
        new_fee_paid = TRUE
        OR (
            reason IS NOT NULL
            AND btrim(reason) <> ''
        )
    )
);

CREATE INDEX idx_registration_fee_events_registration_created_at
ON registration_fee_events (registration_id, created_at DESC);

CREATE INDEX idx_registration_fee_events_changed_by_created_at
ON registration_fee_events (changed_by_admin_id, created_at DESC);

CREATE INDEX idx_registrations_fee_paid
ON registrations (fee_paid, id);
