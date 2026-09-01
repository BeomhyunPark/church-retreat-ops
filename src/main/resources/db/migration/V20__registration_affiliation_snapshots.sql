-- Store participant-provided church affiliation snapshots directly.
ALTER TABLE registrations
    ADD COLUMN middle_group_name VARCHAR(100),
    ADD COLUMN cell_name VARCHAR(100);

UPDATE registrations r
SET middle_group_name = NULLIF(BTRIM(mg.name), ''),
    cell_name = COALESCE(NULLIF(BTRIM(cc.name), ''), NULLIF(BTRIM(r.church_cell_department), ''))
FROM church_cells cc
JOIN church_middle_groups mg ON mg.id = cc.church_middle_group_id
WHERE r.church_cell_id = cc.id;

UPDATE registrations
SET cell_name = NULLIF(BTRIM(church_cell_department), '')
WHERE cell_name IS NULL;

CREATE INDEX idx_registrations_affiliation_names
ON registrations (retreat_id, middle_group_name, cell_name);
