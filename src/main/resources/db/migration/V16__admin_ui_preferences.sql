ALTER TABLE admin_users
    ADD COLUMN ui_preferences JSONB NOT NULL DEFAULT '{}'::jsonb;
