CREATE TABLE IF NOT EXISTS economy.transactions (
    transaction_id BIGSERIAL PRIMARY KEY,
    account_id INTEGER NOT NULL REFERENCES accounts.account(account_id) ON DELETE CASCADE,
    type TEXT NOT NULL,
    amount INTEGER NOT NULL,
    balance_after INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
