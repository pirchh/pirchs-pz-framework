CREATE SCHEMA IF NOT EXISTS ownership;

CREATE TABLE IF NOT EXISTS ownership.node (
    node_key TEXT PRIMARY KEY,
    node_type TEXT NOT NULL DEFAULT 'generic',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
