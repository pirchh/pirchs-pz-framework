ALTER TABLE accounts.account
    ADD COLUMN IF NOT EXISTS canonical_external_id TEXT,
    ADD COLUMN IF NOT EXISTS player_source TEXT,
    ADD COLUMN IF NOT EXISTS source_player_id TEXT,
    ADD COLUMN IF NOT EXISTS steam_id TEXT,
    ADD COLUMN IF NOT EXISTS online_id_last_seen TEXT,
    ADD COLUMN IF NOT EXISTS username_last_seen TEXT,
    ADD COLUMN IF NOT EXISTS display_name_last_seen TEXT,
    ADD COLUMN IF NOT EXISTS character_forename_last_seen TEXT,
    ADD COLUMN IF NOT EXISTS character_surname_last_seen TEXT,
    ADD COLUMN IF NOT EXISTS character_full_name_last_seen TEXT,
    ADD COLUMN IF NOT EXISTS identity_last_seen_at TIMESTAMPTZ;

UPDATE accounts.account
SET canonical_external_id = external_id
WHERE canonical_external_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_accounts_account_canonical_external_id
    ON accounts.account (canonical_external_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_accounts_account_steam_id
    ON accounts.account (steam_id)
    WHERE steam_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_accounts_account_username_last_seen
    ON accounts.account (username_last_seen);

CREATE INDEX IF NOT EXISTS ix_accounts_account_display_name_last_seen
    ON accounts.account (display_name_last_seen);

CREATE INDEX IF NOT EXISTS ix_accounts_account_character_full_name_last_seen
    ON accounts.account (character_full_name_last_seen);
