ALTER TABLE registrations
    ADD COLUMN attendance_type VARCHAR(20) NOT NULL DEFAULT 'FULL',
    ADD COLUMN transportation_type VARCHAR(30) NOT NULL DEFAULT 'UNDECIDED',
    ADD COLUMN carpool_needed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN carpool_offer BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN carpool_seats SMALLINT,
    ADD COLUMN transportation_note VARCHAR(200),
    ADD CONSTRAINT ck_registrations_attendance_type CHECK (attendance_type IN ('FULL', 'PARTIAL')),
    ADD CONSTRAINT ck_registrations_transportation_type CHECK (
        transportation_type IN ('OWN_CAR', 'PUBLIC_TRANSPORT', 'UNDECIDED')
    ),
    ADD CONSTRAINT ck_registrations_carpool_seats CHECK (carpool_seats IS NULL OR carpool_seats BETWEEN 0 AND 20);

CREATE TABLE registration_attendance_slots (
    registration_id BIGINT NOT NULL REFERENCES registrations (id) ON DELETE CASCADE,
    slot_code VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (registration_id, slot_code),
    CONSTRAINT ck_registration_attendance_slots_code CHECK (
        slot_code IN (
            'DAY1_MORNING',
            'DAY1_AFTERNOON',
            'DAY1_GATHERING',
            'DAY2_MORNING',
            'DAY2_AFTERNOON',
            'DAY2_GATHERING',
            'DAY3_MORNING',
            'DAY3_AFTERNOON',
            'DAY3_GATHERING'
        )
    )
);

CREATE INDEX idx_registration_attendance_slots_slot_code
    ON registration_attendance_slots (slot_code);
