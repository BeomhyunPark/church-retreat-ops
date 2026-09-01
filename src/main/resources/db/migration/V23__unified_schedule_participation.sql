-- Make the timetable the source of truth for participation collection.
ALTER TABLE retreat_schedule_items
    ALTER COLUMN starts_at DROP NOT NULL,
    ALTER COLUMN ends_at DROP NOT NULL,
    ALTER COLUMN created_by_admin_id DROP NOT NULL,
    ALTER COLUMN updated_by_admin_id DROP NOT NULL;

ALTER TABLE retreat_schedule_items
    DROP CONSTRAINT ck_retreat_schedule_items_time_range;

ALTER TABLE retreat_schedule_items
    ADD CONSTRAINT ck_retreat_schedule_items_time_range CHECK (
        (starts_at IS NULL AND ends_at IS NULL)
        OR (starts_at IS NOT NULL AND ends_at IS NOT NULL AND ends_at > starts_at)
    );

ALTER TABLE retreat_schedule_items
    DROP CONSTRAINT ck_retreat_schedule_items_category;

ALTER TABLE retreat_schedule_items
    ADD CONSTRAINT ck_retreat_schedule_items_category CHECK (
        category IN (
            'PROGRAM',
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
    );

ALTER TABLE retreat_schedule_items
    ADD COLUMN collect_participation BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE retreat_participation_options
    ADD COLUMN schedule_item_id BIGINT UNIQUE REFERENCES retreat_schedule_items (id);

-- V21 created the first template from the then-current retreat dates. If that
-- same retreat was later rescheduled, move the whole template as a set while
-- preserving option ids and participant selections.
WITH current_anchor AS (
    SELECT
        r.id AS retreat_id,
        r.starts_on,
        r.ends_on,
        MIN(option.event_date) AS option_starts_on
    FROM retreats r
    JOIN retreat_participation_options option ON option.retreat_id = r.id
    WHERE r.status IN ('DRAFT', 'OPEN')
    GROUP BY r.id, r.starts_on, r.ends_on
), rebased AS (
    SELECT
        option.id,
        anchor.starts_on + (option.event_date - anchor.option_starts_on) AS next_event_date,
        anchor.starts_on,
        anchor.ends_on
    FROM retreat_participation_options option
    JOIN current_anchor anchor ON anchor.retreat_id = option.retreat_id
    WHERE anchor.option_starts_on < anchor.starts_on
       OR anchor.option_starts_on > anchor.ends_on
)
UPDATE retreat_participation_options option
SET event_date = rebased.next_event_date,
    is_active = option.is_active
        AND rebased.next_event_date BETWEEN rebased.starts_on AND rebased.ends_on,
    updated_at = now()
FROM rebased
WHERE option.id = rebased.id;

-- Existing dynamic options become time-unspecified schedule rows. Nullable
-- actor columns distinguish migration-generated rows from administrator edits.
INSERT INTO retreat_schedule_items (
    retreat_id,
    title,
    schedule_date,
    starts_at,
    ends_at,
    category,
    target_audience,
    is_active,
    display_order,
    collect_participation,
    created_by_admin_id,
    updated_by_admin_id,
    created_at,
    updated_at
)
SELECT
    option.retreat_id,
    option.label,
    option.event_date,
    NULL,
    NULL,
    CASE WHEN option.option_type = 'MEAL' THEN 'MEAL' ELSE 'PROGRAM' END,
    'ALL',
    option.is_active,
    option.display_order,
    TRUE,
    NULL,
    NULL,
    option.created_at,
    option.updated_at
FROM retreat_participation_options option
WHERE option.schedule_item_id IS NULL;

UPDATE retreat_participation_options option
SET schedule_item_id = schedule.id
FROM retreat_schedule_items schedule
WHERE option.schedule_item_id IS NULL
  AND schedule.retreat_id = option.retreat_id
  AND schedule.title = option.label
  AND schedule.schedule_date = option.event_date
  AND schedule.display_order = option.display_order
  AND schedule.collect_participation = TRUE
  AND schedule.created_by_admin_id IS NULL;
