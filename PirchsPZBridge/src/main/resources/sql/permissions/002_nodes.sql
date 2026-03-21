CREATE TABLE IF NOT EXISTS permissions.node (
    node_id SERIAL PRIMARY KEY,
    node_key TEXT UNIQUE NOT NULL,
    mod_scope TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);