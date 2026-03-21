CREATE TABLE IF NOT EXISTS accounts.account (
    account_id SERIAL PRIMARY KEY,
    external_id TEXT UNIQUE NOT NULL,
    account_name TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMP
);