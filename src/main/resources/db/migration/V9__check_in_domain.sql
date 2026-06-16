CREATE TABLE retreat_check_ins (
    id BIGSERIAL PRIMARY KEY,
    participant_id BIGINT NOT NULL REFERENCES registrations (id),
    checked_in BOOLEAN NOT NULL DEFAULT FALSE,
    checked_in_at TIMESTAMPTZ,
    checked_in_by_admin_id BIGINT REFERENCES admin_users (id),
    check_in_method VARCHAR(20),
    cancelled_at TIMESTAMPTZ,
    cancelled_by_admin_id BIGINT REFERENCES admin_users (id),
    cancellation_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_retreat_check_ins_participant UNIQUE (participant_id),
    CONSTRAINT ck_retreat_check_ins_method CHECK (
        check_in_method IS NULL
        OR check_in_method IN ('MANUAL', 'QR')
    ),
    CONSTRAINT ck_retreat_check_ins_checked_in_fields CHECK (
        checked_in = FALSE
        OR (
            checked_in_at IS NOT NULL
            AND checked_in_by_admin_id IS NOT NULL
            AND check_in_method IS NOT NULL
        )
    ),
    CONSTRAINT ck_retreat_check_ins_cancellation_fields CHECK (
        cancelled_at IS NULL
        OR (
            cancelled_by_admin_id IS NOT NULL
            AND cancellation_reason IS NOT NULL
            AND btrim(cancellation_reason) <> ''
        )
    )
);

CREATE TABLE retreat_check_in_events (
    id BIGSERIAL PRIMARY KEY,
    participant_id BIGINT NOT NULL REFERENCES registrations (id),
    action VARCHAR(30) NOT NULL,
    method VARCHAR(20) NOT NULL,
    performed_by_admin_id BIGINT NOT NULL REFERENCES admin_users (id),
    reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_retreat_check_in_events_action CHECK (
        action IN ('CHECKED_IN', 'CANCELLED')
    ),
    CONSTRAINT ck_retreat_check_in_events_method CHECK (
        method IN ('MANUAL', 'QR')
    ),
    CONSTRAINT ck_retreat_check_in_events_cancel_reason CHECK (
        action <> 'CANCELLED'
        OR (
            reason IS NOT NULL
            AND btrim(reason) <> ''
        )
    )
);

CREATE TABLE participant_check_in_tokens (
    id BIGSERIAL PRIMARY KEY,
    participant_id BIGINT NOT NULL REFERENCES registrations (id),
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    issued_by_admin_id BIGINT NOT NULL REFERENCES admin_users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_participant_check_in_tokens_hash UNIQUE (token_hash),
    CONSTRAINT ck_participant_check_in_tokens_expiration CHECK (expires_at > created_at)
);

CREATE INDEX idx_retreat_check_ins_roster
ON retreat_check_ins (checked_in, participant_id);

CREATE INDEX idx_retreat_check_in_events_participant
ON retreat_check_in_events (participant_id, created_at DESC);

CREATE INDEX idx_participant_check_in_tokens_participant
ON participant_check_in_tokens (participant_id, revoked_at, expires_at);

CREATE INDEX idx_participant_check_in_tokens_hash
ON participant_check_in_tokens (token_hash);

CREATE INDEX idx_participant_check_in_tokens_expires_at
ON participant_check_in_tokens (expires_at);
