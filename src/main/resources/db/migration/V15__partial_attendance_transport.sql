ALTER TABLE registrations
    ADD COLUMN planned_arrival_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN planned_departure_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN partial_attendance_note VARCHAR(300),
    ADD COLUMN inbound_worship_bus_ride_slot VARCHAR(30),
    ADD COLUMN outbound_worship_bus_ride_slot VARCHAR(30),
    ADD COLUMN inbound_carpool_route_area VARCHAR(100),
    ADD COLUMN outbound_carpool_route_area VARCHAR(100);

ALTER TABLE registrations
    ADD CONSTRAINT ck_registrations_inbound_worship_bus_ride_slot
        CHECK (inbound_worship_bus_ride_slot IS NULL
            OR inbound_worship_bus_ride_slot IN ('DAY1_BEFORE_WORSHIP', 'DAY2_BEFORE_WORSHIP')),
    ADD CONSTRAINT ck_registrations_outbound_worship_bus_ride_slot
        CHECK (outbound_worship_bus_ride_slot IS NULL
            OR outbound_worship_bus_ride_slot IN ('DAY1_AFTER_WORSHIP', 'DAY2_AFTER_WORSHIP'));
