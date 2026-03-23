CREATE SCHEMA IF NOT EXISTS permissions;

CREATE TABLE IF NOT EXISTS permissions.account_permission (
    account_id INTEGER NOT NULL,
    permission_key TEXT NOT NULL,
    scope_type TEXT NULL,
    scope_key TEXT NULL,
    granted_by_account_id INTEGER NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_permissions_account_permission_account
        FOREIGN KEY (account_id) REFERENCES accounts.account(account_id),
    CONSTRAINT fk_permissions_account_permission_granted_by
        FOREIGN KEY (granted_by_account_id) REFERENCES accounts.account(account_id),
    CONSTRAINT ux_permissions_account_permission
        UNIQUE (account_id, permission_key, scope_type, scope_key)
);

CREATE INDEX IF NOT EXISTS idx_permissions_account_permission_account_id
    ON permissions.account_permission(account_id);

CREATE INDEX IF NOT EXISTS idx_permissions_account_permission_lookup
    ON permissions.account_permission(account_id, permission_key, scope_type, scope_key);

CREATE INDEX IF NOT EXISTS idx_permissions_account_permission_active
    ON permissions.account_permission(active);
