ALTER TABLE registrations
    ADD COLUMN attendance_type VARCHAR(20),
    ADD COLUMN transportation_method VARCHAR(20),
    ADD COLUMN carpool_available BOOLEAN,
    ADD COLUMN carpool_seats SMALLINT,
    ADD COLUMN lodging_night1 BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN lodging_night2 BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN attend_day1_morning   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN attend_day1_afternoon BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN attend_day1_worship   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN attend_day2_morning   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN attend_day2_afternoon BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN attend_day2_worship   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN attend_day3_morning   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN attend_day3_afternoon BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill rows that predate this feature: treat as full attendees on
-- group transport, present (and staying) for the whole retreat.
UPDATE registrations
SET attendance_type = 'FULL',
    transportation_method = 'BUS',
    lodging_night1 = TRUE, lodging_night2 = TRUE,
    attend_day1_morning = TRUE, attend_day1_afternoon = TRUE, attend_day1_worship = TRUE,
    attend_day2_morning = TRUE, attend_day2_afternoon = TRUE, attend_day2_worship = TRUE,
    attend_day3_morning = TRUE, attend_day3_afternoon = TRUE
WHERE attendance_type IS NULL;

ALTER TABLE registrations
    ALTER COLUMN attendance_type SET NOT NULL,
    ALTER COLUMN transportation_method SET NOT NULL;

ALTER TABLE registrations
    ADD CONSTRAINT ck_registrations_attendance_type
        CHECK (attendance_type IN ('FULL', 'PARTIAL', 'WORSHIP_ONLY')),
    ADD CONSTRAINT ck_registrations_transportation_method
        CHECK (transportation_method IN ('OWN_CAR', 'BUS', 'PUBLIC_TRANSIT', 'RIDE_NEEDED'));
