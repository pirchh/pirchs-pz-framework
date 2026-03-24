# Pirchs PZ DBI

## Debug test bridge add-on overwrite

This overwrite pass adds a temporary but useful debug bridge for manually testing the auth surface after the backend hardening pass.

### What this adds

- `pz.bridge.debug.selfTestNow`
- `pz.bridge.debug.selfTestStatus`
- `pz.bridge.debug.resetLifecycle`
- `pz.bridge.debug.claimNode`
- `pz.bridge.debug.releaseNode`
- `pz.bridge.debug.getNodeOwner`
- `pz.bridge.debug.listOwnedNodes`
- `pz.bridge.debug.grantPermissionToSelf`
- `pz.bridge.debug.revokePermissionFromSelf`
- `pz.bridge.debug.hasPermission`
- `pz.bridge.debug.explainPermission`
- `pz.bridge.debug.listPermissions`
- `pz.bridge.debug.assignRoleToSelf`
- `pz.bridge.debug.revokeRoleFromSelf`
- `pz.bridge.debug.hasRole`
- `pz.bridge.debug.listRoles`
- `pz.bridge.debug.localSnapshot`

### Why this exists

The Java backend already exposes the real bridge methods for ownership, permissions, roles, and auth self-test. What this overwrite adds is a convenient local-account test layer so you can manually exercise those paths without having to hand-edit the database every time.

### Important behavior

These debug methods assume the local player has already been resolved by the identity lifecycle watcher.

That means the normal boot sequence still matters:

1. start the game with the java agent
2. let the world finish loading
3. wait for the lifecycle log line showing the local account resolved
4. then invoke debug methods

### Config

Keep this enabled while testing:

```properties
debug.bridge.enabled=true
auth.selftest.enabled=true
```

You can turn the debug bridge off later with:

```properties
debug.bridge.enabled=false
```

See `docs/DEBUG_AUTH_TESTING.md` for the recommended test flow and suggested manual test sequence.
