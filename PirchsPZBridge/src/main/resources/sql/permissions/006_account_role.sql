CREATE TABLE IF NOT EXISTS permissions.account_role (
    account_id INTEGER NOT NULL REFERENCES accounts.account(account_id) ON DELETE CASCADE,
    role_key TEXT NOT NULL REFERENCES permissions.role(role_key) ON DELETE CASCADE,
    scope_type TEXT NULL,
    scope_key TEXT NULL,
    granted_by_account_id INTEGER NULL REFERENCES accounts.account(account_id) ON DELETE SET NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_account_role_scope
ON permissions.account_role (
    account_id,
    role_key,
    COALESCE(scope_type, ''),
    COALESCE(scope_key, '')
);
