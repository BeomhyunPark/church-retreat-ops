-- Carpool pickup-area fields: where an OWN_CAR driver with room to spare can pick
-- people up, and where a CARPOOL_NEEDED rider would prefer to be picked up.
ALTER TABLE registrations
    ADD COLUMN inbound_carpool_area VARCHAR(100),
    ADD COLUMN inbound_carpool_preferred_area VARCHAR(100),
    ADD COLUMN outbound_carpool_area VARCHAR(100),
    ADD COLUMN outbound_carpool_preferred_area VARCHAR(100);
