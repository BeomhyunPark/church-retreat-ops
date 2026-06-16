CREATE TABLE retreat_groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE retreat_group_members (
    id BIGSERIAL PRIMARY KEY,
    retreat_group_id BIGINT NOT NULL REFERENCES retreat_groups (id),
    registration_id BIGINT NOT NULL REFERENCES registrations (id) ON DELETE CASCADE,
    leader BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_retreat_group_members_registration UNIQUE (registration_id),
    CONSTRAINT ux_retreat_group_members_group_registration UNIQUE (retreat_group_id, registration_id)
);

CREATE UNIQUE INDEX ux_retreat_group_members_one_leader_per_group
ON retreat_group_members (retreat_group_id)
WHERE leader = TRUE;

CREATE INDEX idx_retreat_groups_active_order
ON retreat_groups (active, display_order, name);

CREATE INDEX idx_retreat_group_members_group
ON retreat_group_members (retreat_group_id, leader, registration_id);

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
            'ADMIN_MANAGEMENT_UPDATED',
            'CHURCH_CELL_UPDATED',
            'RETREAT_GROUP_UPDATED'
        )
    );
