-- Introduce the single active retreat lifecycle after production QR migrations.
CREATE TABLE retreats (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    participant_count INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_retreats_date_range CHECK (ends_on >= starts_on),
    CONSTRAINT ck_retreats_status CHECK (status IN ('DRAFT', 'OPEN', 'CLOSED')),
    CONSTRAINT ck_retreats_participant_count CHECK (
        participant_count IS NULL OR participant_count >= 0
    ),
    CONSTRAINT ck_retreats_closed_summary CHECK (
        status <> 'CLOSED' OR participant_count IS NOT NULL
    )
);

CREATE UNIQUE INDEX ux_retreats_one_current
ON retreats ((TRUE))
WHERE status IN ('DRAFT', 'OPEN');

INSERT INTO retreats (name, starts_on, ends_on, status)
SELECT
    'Existing Retreat',
    COALESCE(MIN(schedule_date), CURRENT_DATE),
    COALESCE(MAX(schedule_date), CURRENT_DATE),
    'OPEN'
FROM retreat_schedule_items;

ALTER TABLE registrations
    ADD COLUMN retreat_id BIGINT REFERENCES retreats (id);

ALTER TABLE retreat_groups
    ADD COLUMN retreat_id BIGINT REFERENCES retreats (id);

ALTER TABLE announcements
    ADD COLUMN retreat_id BIGINT REFERENCES retreats (id);

ALTER TABLE retreat_schedule_items
    ADD COLUMN retreat_id BIGINT REFERENCES retreats (id);

UPDATE registrations
SET retreat_id = (SELECT id FROM retreats WHERE status = 'OPEN');

UPDATE retreat_groups
SET retreat_id = (SELECT id FROM retreats WHERE status = 'OPEN');

UPDATE announcements
SET retreat_id = (SELECT id FROM retreats WHERE status = 'OPEN');

UPDATE retreat_schedule_items
SET retreat_id = (SELECT id FROM retreats WHERE status = 'OPEN');

ALTER TABLE registrations
    ALTER COLUMN retreat_id SET NOT NULL;

ALTER TABLE retreat_groups
    ALTER COLUMN retreat_id SET NOT NULL;

ALTER TABLE announcements
    ALTER COLUMN retreat_id SET NOT NULL;

ALTER TABLE retreat_schedule_items
    ALTER COLUMN retreat_id SET NOT NULL;

DROP INDEX ux_registrations_name_phone_registered;

CREATE UNIQUE INDEX ux_registrations_retreat_name_phone_registered
ON registrations (retreat_id, normalized_name, phone_number)
WHERE status = 'REGISTERED';

ALTER TABLE retreat_groups
    DROP CONSTRAINT retreat_groups_name_key;

CREATE UNIQUE INDEX ux_retreat_groups_retreat_name
ON retreat_groups (retreat_id, name);

CREATE INDEX idx_registrations_retreat_status_created_at
ON registrations (retreat_id, status, created_at DESC);

CREATE INDEX idx_retreat_groups_retreat_active_order
ON retreat_groups (retreat_id, active, display_order, name);

CREATE INDEX idx_announcements_retreat_created_at
ON announcements (retreat_id, created_at DESC);

CREATE INDEX idx_retreat_schedule_items_retreat_list
ON retreat_schedule_items (retreat_id, schedule_date, display_order, starts_at, id);
