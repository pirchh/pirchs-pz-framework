CREATE TABLE IF NOT EXISTS accounts.account (
    account_id SERIAL PRIMARY KEY,
    external_id TEXT NOT NULL UNIQUE,
    canonical_external_id TEXT NOT NULL UNIQUE,
    player_source TEXT NULL,
    source_player_id TEXT NULL,
    steam_id TEXT NULL,
    account_name TEXT NULL,
    username_last_seen TEXT NULL,
    display_name_last_seen TEXT NULL,
    online_id_last_seen TEXT NULL,
    character_forename_last_seen TEXT NULL,
    character_surname_last_seen TEXT NULL,
    character_full_name_last_seen TEXT NULL,
    character_external_id_last_seen TEXT NULL,
    identity_last_seen_at TIMESTAMPTZ NULL,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
