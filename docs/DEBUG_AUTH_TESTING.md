# Debug Auth Testing

This overwrite adds a local-account debug bridge so you can manually test ownership, permissions, roles, and the auth self-test after boot.

## Goal

After the local player resolves, you should be able to manually exercise the backend without relying only on the automatic startup self-test.

## Added debug methods

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

## Suggested config while testing

```properties
auth.selftest.enabled=true
auth.selftest.node_key=debug:test-node
auth.selftest.node_type=node
auth.selftest.permission_key=ownership.manage
auth.selftest.scope_type=node
auth.selftest.scope_key=debug:test-node
auth.selftest.teardown=true
debug.bridge.enabled=true
```

## First validation after boot

Wait until the local player resolves in logs, then verify:

- `pz.bridge.system.healthCheck`
- `pz.bridge.system.listMethods`
- `pz.bridge.player.getLifecycleState`
- `pz.bridge.debug.localSnapshot`
- `pz.bridge.debug.selfTestStatus`

If `localSnapshot` says no local account has been resolved yet, wait until the save fully loads.

## Recommended manual test sequence

Use a fresh node key such as `debug:test-node-a`.

### 1. Claim a node
- call `pz.bridge.debug.claimNode("debug:test-node-a", "node")`
- then call `pz.bridge.debug.getNodeOwner("debug:test-node-a")`
- then call `pz.bridge.debug.listOwnedNodes()`

Expected result:
- claim succeeds
- owner points at the local account
- owned node list includes the test node

### 2. Owner-implied permission check
- call `pz.bridge.debug.explainPermission("ownership.manage", "node", "debug:test-node-a")`
- call `pz.bridge.debug.hasPermission("ownership.manage", "node", "debug:test-node-a")`

Expected result:
- allowed is true
- reason is owner implied or equivalent

### 3. Negative permission check
- call `pz.bridge.debug.explainPermission("business.manage", "node", "debug:test-node-a")`
- call `pz.bridge.debug.hasPermission("business.manage", "node", "debug:test-node-a")`

Expected result:
- denied unless you have a direct grant or a role that implies it

### 4. Scoped permission grant to self
Use a fresh node key such as `debug:test-node-b`.

- claim `debug:test-node-b`
- call `pz.bridge.debug.grantPermissionToSelf("business.manage", "node", "debug:test-node-b")`
- call `pz.bridge.debug.hasPermission("business.manage", "node", "debug:test-node-b")`
- call `pz.bridge.debug.explainPermission("business.manage", "node", "debug:test-node-b")`
- call `pz.bridge.debug.listPermissions()`

Expected result:
- the local account can grant to itself if it is owner and the permission is scoped to that node for `permissions.manage.scope` flows that the owner can manage
- the explain result should become allowed

Note:
if your policy intentionally blocks self-grant of arbitrary keys, that is a design choice. In that case the method should fail with a clear authorization message rather than silently doing nothing.

### 5. Scoped role assignment to self
Use a fresh node key such as `debug:test-node-c`.

- claim `debug:test-node-c`
- call `pz.bridge.debug.assignRoleToSelf("business_manager", "node", "debug:test-node-c")`
- call `pz.bridge.debug.hasRole("business_manager", "node", "debug:test-node-c")`
- call `pz.bridge.debug.listRoles()`
- call `pz.bridge.debug.explainPermission("business.manage", "node", "debug:test-node-c")`

Expected result:
- the role assignment succeeds if owner-scoped role assignment is allowed for that node
- `business.manage` becomes allowed through the role implication path

### 6. Cleanup
- call `pz.bridge.debug.revokePermissionFromSelf("business.manage", "node", "debug:test-node-b")`
- call `pz.bridge.debug.revokeRoleFromSelf("business_manager", "node", "debug:test-node-c")`
- call `pz.bridge.debug.releaseNode("debug:test-node-a")`
- call `pz.bridge.debug.releaseNode("debug:test-node-b")`
- call `pz.bridge.debug.releaseNode("debug:test-node-c")`

## If you cannot invoke methods directly yet

This overwrite gives you the Java-side methods. You still need whatever Lua or UI-side invoker your environment uses to call the loader dispatcher.

If you do not yet have that wired, you can still test the automatic path by changing `auth.selftest.*` config keys and rebooting.
