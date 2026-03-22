ALTER TABLE IF EXISTS accounts.account
    ADD COLUMN IF NOT EXISTS player_source VARCHAR(64),
    ADD COLUMN IF NOT EXISTS source_player_id VARCHAR(128),
    ADD COLUMN IF NOT EXISTS steam_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS username VARCHAR(128),
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(128);

CREATE UNIQUE INDEX IF NOT EXISTS ux_accounts_account_player_source_source_player_id
    ON accounts.account (player_source, source_player_id)
    WHERE player_source IS NOT NULL AND source_player_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_accounts_account_steam_id
    ON accounts.account (steam_id)
    WHERE steam_id IS NOT NULL;
