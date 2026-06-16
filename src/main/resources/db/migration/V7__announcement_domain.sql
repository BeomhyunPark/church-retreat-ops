CREATE TABLE announcements (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    content TEXT NOT NULL,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    visible_from TIMESTAMPTZ,
    visible_until TIMESTAMPTZ,
    created_by_admin_id BIGINT NOT NULL REFERENCES admin_users (id),
    updated_by_admin_id BIGINT NOT NULL REFERENCES admin_users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_announcements_visible_period CHECK (
        visible_from IS NULL
        OR visible_until IS NULL
        OR visible_until >= visible_from
    )
);

CREATE TABLE announcement_targets (
    id BIGSERIAL PRIMARY KEY,
    announcement_id BIGINT NOT NULL REFERENCES announcements (id) ON DELETE CASCADE,
    target_type VARCHAR(50) NOT NULL,
    target_value VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_announcement_targets_type CHECK (
        target_type IN (
            'ALL',
            'REGISTRATION_STATUS',
            'PAYMENT_STATUS',
            'NEWCOMER',
            'CARE_TARGET',
            'CHURCH_MIDDLE_GROUP',
            'CHURCH_CELL',
            'RETREAT_GROUP',
            'ADMIN_ROLE'
        )
    ),
    CONSTRAINT ck_announcement_targets_all_value CHECK (
        target_type <> 'ALL'
        OR target_value IS NULL
    )
);

CREATE UNIQUE INDEX ux_announcement_targets_equivalent_target
ON announcement_targets (announcement_id, target_type, COALESCE(target_value, ''));

CREATE INDEX idx_announcements_active_visible_pinned
ON announcements (is_active, is_pinned, visible_from, visible_until, created_at);

CREATE INDEX idx_announcement_targets_announcement
ON announcement_targets (announcement_id, target_type);
