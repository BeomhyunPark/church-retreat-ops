ALTER TABLE registrations
    ADD COLUMN inbound_carpool_note VARCHAR(200),
    ADD COLUMN inbound_carpool_preferred_note VARCHAR(200),
    ADD COLUMN outbound_carpool_note VARCHAR(200),
    ADD COLUMN outbound_carpool_preferred_note VARCHAR(200);
