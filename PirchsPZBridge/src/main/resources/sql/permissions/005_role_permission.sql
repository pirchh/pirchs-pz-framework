CREATE TABLE IF NOT EXISTS permissions.role_permission (
    role_key TEXT NOT NULL REFERENCES permissions.role(role_key) ON DELETE CASCADE,
    permission_key TEXT NOT NULL,
    scope_type TEXT NULL,
    scope_key TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_role_permission_scope
ON permissions.role_permission (
    role_key,
    permission_key,
    COALESCE(scope_type, ''),
    COALESCE(scope_key, '')
);
