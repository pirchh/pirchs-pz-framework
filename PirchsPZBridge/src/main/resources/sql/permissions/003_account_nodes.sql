CREATE TABLE IF NOT EXISTS permissions.account_node (
    account_id INTEGER NOT NULL,
    node_id INTEGER NOT NULL,
    allowed BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (account_id, node_id)
);