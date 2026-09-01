ALTER TABLE retreat_group_members
ADD COLUMN display_order INTEGER;

WITH ranked_members AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY retreat_group_id
            ORDER BY leader DESC, created_at ASC, id ASC
        ) - 1 AS display_order
    FROM retreat_group_members
)
UPDATE retreat_group_members AS member
SET display_order = ranked.display_order
FROM ranked_members AS ranked
WHERE member.id = ranked.id;

ALTER TABLE retreat_group_members
ALTER COLUMN display_order SET NOT NULL;

ALTER TABLE retreat_group_members
ADD CONSTRAINT ck_retreat_group_members_display_order CHECK (display_order >= 0);

CREATE INDEX idx_retreat_group_members_order
ON retreat_group_members (retreat_group_id, display_order, registration_id);
