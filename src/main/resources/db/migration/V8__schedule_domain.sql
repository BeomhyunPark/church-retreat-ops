CREATE TABLE retreat_schedule_items (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    schedule_date DATE NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    location VARCHAR(150),
    category VARCHAR(50) NOT NULL,
    target_audience VARCHAR(50) NOT NULL DEFAULT 'ALL',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_by_admin_id BIGINT NOT NULL REFERENCES admin_users (id),
    updated_by_admin_id BIGINT NOT NULL REFERENCES admin_users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_retreat_schedule_items_time_range CHECK (ends_at > starts_at),
    CONSTRAINT ck_retreat_schedule_items_display_order CHECK (display_order >= 0),
    CONSTRAINT ck_retreat_schedule_items_category CHECK (
        category IN (
            'WORSHIP',
            'PRAYER',
            'MEAL',
            'GROUP_ACTIVITY',
            'LECTURE',
            'BREAK',
            'MOVE',
            'CHECK_IN',
            'CHECK_OUT',
            'NOTICE',
            'ETC'
        )
    ),
    CONSTRAINT ck_retreat_schedule_items_target_audience CHECK (
        target_audience IN (
            'ALL',
            'STAFF_ONLY',
            'LEADERS_ONLY',
            'NEWCOMERS',
            'CARE_TARGETS'
        )
    )
);

CREATE INDEX idx_retreat_schedule_items_list
ON retreat_schedule_items (schedule_date, is_active, category, display_order, starts_at, id);
