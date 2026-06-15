CREATE TABLE church_middle_groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    elder_name VARCHAR(100),
    description TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE church_cells (
    id BIGSERIAL PRIMARY KEY,
    church_middle_group_id BIGINT NOT NULL REFERENCES church_middle_groups (id),
    name VARCHAR(100) NOT NULL,
    cell_leader_name VARCHAR(100),
    description TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_church_cells_middle_group_name UNIQUE (church_middle_group_id, name)
);

CREATE INDEX idx_church_middle_groups_active_order
ON church_middle_groups (active, display_order, name);

CREATE INDEX idx_church_cells_middle_group_active_order
ON church_cells (church_middle_group_id, active, display_order, name);

ALTER TABLE registrations
    ADD COLUMN church_cell_id BIGINT NULL REFERENCES church_cells (id);

CREATE INDEX idx_registrations_church_cell_id
ON registrations (church_cell_id);

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
            'CHURCH_CELL_UPDATED'
        )
    );
