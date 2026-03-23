# Java-side Auth Self-Test

This patch replaces the temporary Lua-triggered auth test harness with a Java-side self-test that runs after the local account resolves.

## Why

The ownership and permissions foundation is intended to be consumed by Java menus and Java hooks, not by ad hoc Lua debug triggers. The earlier Lua harness proved brittle because:

- Project Zomboid debug mode already binds many function keys.
- Lua event timing was noisy and hard to verify.
- The actual production path for these systems is Java-side anyway.

## What this self-test does

Once the local lifecycle resolves an account for the current session, the bridge will run a one-shot self-test that:

1. claims a debug node for the resolved account
2. reads the node owner back
3. lists the account's owned nodes
4. grants a scoped debug permission to the same account
5. verifies that permission
6. lists the account's active permissions

## Default config

These properties live in `pirchdb.properties`:

- `pirch.auth.selftest.enabled=true`
- `pirch.auth.selftest.node_key=debug:test-node`
- `pirch.auth.selftest.node_type=debug`
- `pirch.auth.selftest.permission_key=ui.debug.open`
- `pirch.auth.selftest.scope_type=node`
- `pirch.auth.selftest.scope_key=` (blank means fall back to the node key)

## Expected logs

On startup:

- `[PZLIFE][AUTH][selftest] Java-side self-test enabled. It will run once after local account resolution each session.`

After lifecycle resolution:

- `[PZLIFE][AUTH][selftest] ok. accountId=..., nodeKey=..., permissionKey=..., ...`

If something fails:

- `[PZLIFE][AUTH][selftest] failed: ...`

## Manual bridge methods

This patch also adds:

- `pz.bridge.system.runAuthSelfTest`
- `pz.bridge.system.getAuthSelfTestStatus`

These are useful later for Java-driven menus or admin panels.
