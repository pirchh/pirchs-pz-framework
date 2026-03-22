CREATE TABLE IF NOT EXISTS economy.wallet (
    account_id INTEGER PRIMARY KEY REFERENCES accounts.account(account_id) ON DELETE CASCADE,
    balance INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
