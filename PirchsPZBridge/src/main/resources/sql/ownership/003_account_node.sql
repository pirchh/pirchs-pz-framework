CREATE TABLE IF NOT EXISTS ownership.account_node (
    account_id INTEGER NOT NULL,
    node_key TEXT NOT NULL,
    node_type TEXT NOT NULL DEFAULT 'generic',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (node_key),
    CONSTRAINT fk_ownership_account_node_account
        FOREIGN KEY (account_id) REFERENCES accounts.account(account_id),
    CONSTRAINT fk_ownership_account_node_node
        FOREIGN KEY (node_key) REFERENCES ownership.node(node_key)
);

CREATE INDEX IF NOT EXISTS idx_ownership_account_node_account_id
    ON ownership.account_node(account_id);

CREATE INDEX IF NOT EXISTS idx_ownership_account_node_active
    ON ownership.account_node(active);
