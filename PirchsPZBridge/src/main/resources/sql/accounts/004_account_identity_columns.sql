ALTER TABLE accounts.account
    ADD COLUMN IF NOT EXISTS player_source TEXT NULL,
    ADD COLUMN IF NOT EXISTS source_player_id TEXT NULL,
    ADD COLUMN IF NOT EXISTS steam_id TEXT NULL,
    ADD COLUMN IF NOT EXISTS username_last_seen TEXT NULL,
    ADD COLUMN IF NOT EXISTS display_name_last_seen TEXT NULL,
    ADD COLUMN IF NOT EXISTS online_id_last_seen TEXT NULL,
    ADD COLUMN IF NOT EXISTS character_forename_last_seen TEXT NULL,
    ADD COLUMN IF NOT EXISTS character_surname_last_seen TEXT NULL,
    ADD COLUMN IF NOT EXISTS character_full_name_last_seen TEXT NULL,
    ADD COLUMN IF NOT EXISTS character_external_id_last_seen TEXT NULL,
    ADD COLUMN IF NOT EXISTS identity_last_seen_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
