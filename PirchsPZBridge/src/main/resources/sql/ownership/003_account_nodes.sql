CREATE TABLE IF NOT EXISTS ownership.account_node (
    node_key TEXT PRIMARY KEY,
    account_id INTEGER NOT NULL REFERENCES accounts.account(account_id) ON DELETE CASCADE,
    node_type TEXT NOT NULL DEFAULT 'generic',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
