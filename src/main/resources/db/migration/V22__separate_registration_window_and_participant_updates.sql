-- Separate new registration availability from participant self-editing.
ALTER TABLE retreats
    ADD COLUMN registration_open BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE retreats
SET registration_open = TRUE
WHERE status = 'OPEN';

ALTER TABLE retreats
    ADD CONSTRAINT ck_retreats_registration_open_status
        CHECK (NOT registration_open OR status = 'OPEN');

ALTER TABLE registrations
    ADD COLUMN participant_updated_at TIMESTAMPTZ;

UPDATE registrations registration
SET participant_updated_at = participant_history.last_updated_at
FROM (
    SELECT registration_id, MAX(created_at) AS last_updated_at
    FROM registration_histories
    WHERE actor_type = 'PARTICIPANT'
      AND change_type IN ('OVERWRITTEN', 'SELF_UPDATED')
    GROUP BY registration_id
) participant_history
WHERE participant_history.registration_id = registration.id;

CREATE INDEX idx_registrations_retreat_participant_updated
ON registrations (retreat_id, participant_updated_at DESC)
WHERE participant_updated_at IS NOT NULL;
