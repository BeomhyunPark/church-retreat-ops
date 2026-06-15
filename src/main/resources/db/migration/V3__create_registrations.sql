CREATE TABLE registrations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    normalized_name VARCHAR(50) NOT NULL,
    gender VARCHAR(20) NOT NULL,
    birth_year SMALLINT NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    phone_last_four CHAR(4) NOT NULL,
    church_cell_department VARCHAR(100),
    lookup_key_hash VARCHAR(255) NOT NULL,
    privacy_consent_agreed BOOLEAN NOT NULL,
    fee_paid BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_registrations_gender CHECK (gender IN ('MALE', 'FEMALE')),
    CONSTRAINT ck_registrations_status CHECK (status IN ('REGISTERED', 'CANCELLED')),
    CONSTRAINT ck_registrations_phone_last_four CHECK (phone_last_four ~ '^[0-9]{4}$'),
    CONSTRAINT ck_registrations_phone_number CHECK (phone_number ~ '^[0-9]{10,11}$')
);

CREATE UNIQUE INDEX ux_registrations_name_phone_registered
ON registrations (normalized_name, phone_number)
WHERE status = 'REGISTERED';

CREATE INDEX idx_registrations_status_created_at ON registrations (status, created_at DESC);
CREATE INDEX idx_registrations_self_lookup ON registrations (normalized_name, phone_last_four)
WHERE status = 'REGISTERED';

CREATE TABLE registration_histories (
    id BIGSERIAL PRIMARY KEY,
    registration_id BIGINT NOT NULL REFERENCES registrations (id),
    change_type VARCHAR(30) NOT NULL,
    previous_snapshot_json TEXT,
    new_snapshot_json TEXT,
    actor_type VARCHAR(30) NOT NULL,
    actor_admin_user_id BIGINT NULL REFERENCES admin_users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_registration_histories_change_type CHECK (
        change_type IN ('CREATED', 'OVERWRITTEN', 'SELF_UPDATED', 'KEY_REISSUED')
    ),
    CONSTRAINT ck_registration_histories_actor_type CHECK (
        actor_type IN ('PARTICIPANT', 'ADMIN', 'SYSTEM')
    )
);

CREATE INDEX idx_registration_histories_registration_id_created_at
    ON registration_histories (registration_id, created_at DESC);
