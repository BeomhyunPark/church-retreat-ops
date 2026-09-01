CREATE TABLE retreat_participation_options (
    id BIGSERIAL PRIMARY KEY,
    retreat_id BIGINT NOT NULL REFERENCES retreats (id),
    option_type VARCHAR(20) NOT NULL,
    label VARCHAR(100) NOT NULL,
    event_date DATE NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_participation_option_type CHECK (option_type IN ('PROGRAM', 'MEAL')),
    CONSTRAINT ck_participation_option_order CHECK (display_order >= 0),
    CONSTRAINT ux_participation_option_label UNIQUE (retreat_id, event_date, label)
);

CREATE TABLE registration_participation_options (
    registration_id BIGINT NOT NULL REFERENCES registrations (id) ON DELETE CASCADE,
    option_id BIGINT NOT NULL REFERENCES retreat_participation_options (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (registration_id, option_id)
);

CREATE INDEX idx_participation_options_retreat_list
ON retreat_participation_options (retreat_id, is_active, event_date, display_order, id);

CREATE INDEX idx_registration_participation_options_option
ON registration_participation_options (option_id, registration_id);

-- Give an existing retreat the operations team's current three-day template.
-- A shorter retreat only receives options that fall inside its date range.
INSERT INTO retreat_participation_options (
    retreat_id, option_type, label, event_date, display_order
)
SELECT
    r.id,
    template.option_type,
    template.label,
    r.starts_on + template.day_offset,
    template.display_order
FROM retreats r
CROSS JOIN (VALUES
    (0, 'PROGRAM', '오후 프로그램', 10),
    (0, 'MEAL',    '저녁식사',      20),
    (0, 'PROGRAM', '집회',          30),
    (1, 'MEAL',    '아침식사',      40),
    (1, 'PROGRAM', '오전 프로그램', 50),
    (1, 'MEAL',    '점심식사',      60),
    (1, 'PROGRAM', '오후 프로그램', 70),
    (1, 'PROGRAM', '집회',          80),
    (2, 'MEAL',    '아침식사',      90),
    (2, 'PROGRAM', '오전 프로그램', 100),
    (2, 'MEAL',    '점심식사',      110),
    (2, 'PROGRAM', '오후 프로그램', 120)
) AS template(day_offset, option_type, label, display_order)
WHERE r.starts_on + template.day_offset <= r.ends_on;

-- Preserve the intent that can be inferred from the old fixed program fields.
-- Meal attendance cannot be inferred for partial attendees; full attendees select every item.
INSERT INTO registration_participation_options (registration_id, option_id)
SELECT reg.id, option.id
FROM registrations reg
JOIN retreats r ON r.id = reg.retreat_id
JOIN retreat_participation_options option ON option.retreat_id = reg.retreat_id
WHERE reg.attendance_type = 'FULL'
   OR (option.option_type = 'PROGRAM' AND (
          (option.event_date = r.starts_on AND option.label = '오후 프로그램' AND reg.attend_day1_afternoon)
       OR (option.event_date = r.starts_on AND option.label = '집회' AND reg.attend_day1_worship)
       OR (option.event_date = r.starts_on + 1 AND option.label = '오전 프로그램' AND reg.attend_day2_morning)
       OR (option.event_date = r.starts_on + 1 AND option.label = '오후 프로그램' AND reg.attend_day2_afternoon)
       OR (option.event_date = r.starts_on + 1 AND option.label = '집회' AND reg.attend_day2_worship)
       OR (option.event_date = r.starts_on + 2 AND option.label = '오전 프로그램' AND reg.attend_day3_morning)
       OR (option.event_date = r.starts_on + 2 AND option.label = '오후 프로그램' AND reg.attend_day3_afternoon)
   ));
