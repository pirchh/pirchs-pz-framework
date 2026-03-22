CREATE TABLE IF NOT EXISTS permissions.account_permission (
    permission_id BIGSERIAL PRIMARY KEY,
    account_id INTEGER NOT NULL REFERENCES accounts.account(account_id) ON DELETE CASCADE,
    permission_key TEXT NOT NULL,
    scope_type TEXT NULL,
    scope_key TEXT NULL,
    granted_by_account_id INTEGER NULL REFERENCES accounts.account(account_id) ON DELETE SET NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_account_permission_scope
ON permissions.account_permission (
    account_id,
    permission_key,
    COALESCE(scope_type, ''),
    COALESCE(scope_key, '')
);
