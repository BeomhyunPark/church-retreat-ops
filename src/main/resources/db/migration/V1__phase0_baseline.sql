CREATE TABLE app_baseline (
    id SMALLINT PRIMARY KEY,
    description VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT app_baseline_single_row CHECK (id = 1)
);

INSERT INTO app_baseline (id, description)
VALUES (1, 'Phase 0 baseline migration');
