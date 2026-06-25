-- Add new directional transportation fields for inbound and outbound trips
ALTER TABLE registrations
    ADD COLUMN inbound_transportation_method VARCHAR(20),
    ADD COLUMN inbound_carpool_available BOOLEAN,
    ADD COLUMN inbound_carpool_seats SMALLINT,
    ADD COLUMN outbound_transportation_method VARCHAR(20),
    ADD COLUMN outbound_carpool_available BOOLEAN,
    ADD COLUMN outbound_carpool_seats SMALLINT;

-- Backfill from existing single transportation_method field (V11)
-- Both inbound and outbound use the same value as the original transportation_method,
-- remapped to the renamed enum values used by the new directional columns
-- (BUS -> GROUP_BUS, RIDE_NEEDED -> CARPOOL_NEEDED)
UPDATE registrations
SET inbound_transportation_method = CASE transportation_method
        WHEN 'BUS' THEN 'GROUP_BUS'
        WHEN 'RIDE_NEEDED' THEN 'CARPOOL_NEEDED'
        ELSE transportation_method
    END,
    outbound_transportation_method = CASE transportation_method
        WHEN 'BUS' THEN 'GROUP_BUS'
        WHEN 'RIDE_NEEDED' THEN 'CARPOOL_NEEDED'
        ELSE transportation_method
    END,
    inbound_carpool_available = carpool_available,
    outbound_carpool_available = carpool_available,
    inbound_carpool_seats = carpool_seats,
    outbound_carpool_seats = carpool_seats
WHERE transportation_method IS NOT NULL;

-- Make new fields NOT NULL (with the backfilled values)
ALTER TABLE registrations
    ALTER COLUMN inbound_transportation_method SET NOT NULL,
    ALTER COLUMN outbound_transportation_method SET NOT NULL;

-- Add CHECK constraints for new enum values
ALTER TABLE registrations
    ADD CONSTRAINT ck_registrations_inbound_transportation_method
        CHECK (inbound_transportation_method IN ('OWN_CAR', 'GROUP_BUS', 'WORSHIP_SHUTTLE', 'PUBLIC_TRANSIT', 'CARPOOL_NEEDED', 'NOT_DECIDED')),
    ADD CONSTRAINT ck_registrations_outbound_transportation_method
        CHECK (outbound_transportation_method IN ('OWN_CAR', 'GROUP_BUS', 'WORSHIP_SHUTTLE', 'PUBLIC_TRANSIT', 'CARPOOL_NEEDED', 'NOT_DECIDED'));

-- Remove old single transportation method constraints and fields
ALTER TABLE registrations
    DROP CONSTRAINT ck_registrations_transportation_method;

ALTER TABLE registrations
    DROP COLUMN transportation_method,
    DROP COLUMN carpool_available,
    DROP COLUMN carpool_seats;
