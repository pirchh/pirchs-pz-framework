# Bootstrap Seeding Fix

This overwrite patch fixes the startup failure caused by duplicate role-permission seeding.

## What changed

- `PostgresRoleRepository.ensureRolePermission(...)` is now idempotent for existing rows.
- The duplicate check uses PostgreSQL `IS NOT DISTINCT FROM` so `NULL` scope values compare correctly.
- Existing seeded rows now get their `updated_at` touched instead of causing a duplicate-key failure.
- `RoleService.ensureCoreRoles()` is included so the role seed list stays aligned with the patched repository implementation.

## Why startup was failing

Your database already contained this row:

- role_key = `admin`
- permission_key = `permissions.manage`
- scope_type = `NULL`
- scope_key = `NULL`

The earlier seed path tried to insert/update that same logical row in a way that still collided with the
`ux_role_permission_scope` uniqueness rule, which aborted the transaction and killed bootstrap.

## How to apply

Overwrite these files into your repo, rebuild, and reinstall the jars.

## Suggested verification

1. Build.
2. Install to mod folder.
3. Launch the game.
4. Confirm bootstrap no longer dies on `ux_role_permission_scope`.
5. Check `pz.bridge.system.healthCheck`.
6. Check `pz.bridge.system.getAuthSelfTestStatus`.
