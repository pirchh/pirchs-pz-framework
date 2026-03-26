# PirchsPZBridge README

## Module Role

`PirchsPZBridge` is the Java bridge module.

In the current repo state, this module does one concrete thing: it boots the bridge runtime and registers the debug bridge surface that Lua calls into.

That means this module is already functional, but its present scope is specific. It is not a giant service layer yet. Right now it is the debug-bridge registration layer.

---

## Functional Shape

### `pirch.pz.BridgeBootstrap`
`BridgeBootstrap` is the current Java bootstrap target loaded by the agent.

It currently defines:
- `initialize()`
- `isInitialized()`

#### `initialize()`
This method:
1. checks the module-level `initialized` flag,
2. logs and exits if the bridge is already initialized,
3. registers the debug bridge by calling `DebugBridge.register()`,
4. flips the initialized flag,
5. logs completion.

The class-level comment is explicit about the intended split:
- Java-side services and bridge methods initialize here
- Lua exposure happens later from the patched engine `LuaManager.Exposer.exposeAll()`

That separation is already reflected in the repo structure.

---

### `pirch.pz.bridge.DebugBridge`
`DebugBridge` is the actual current bridge surface.

It currently exposes these registered methods:

#### Identity / lifecycle methods
- `pz.bridge.debug.isLocalIdentityReady`
- `pz.bridge.debug.selfTestNow`
- `pz.bridge.debug.selfTestStatus`
- `pz.bridge.debug.resetLifecycle`
- `pz.bridge.debug.localSnapshot`

#### Smoke testing
- `pz.bridge.debug.runSmokeSuite`

#### Node ownership methods
- `pz.bridge.debug.claimNode`
- `pz.bridge.debug.releaseNode`
- `pz.bridge.debug.getNodeOwner`
- `pz.bridge.debug.listOwnedNodes`

#### Permission methods
- `pz.bridge.debug.grantPermissionToSelf`
- `pz.bridge.debug.revokePermissionFromSelf`
- `pz.bridge.debug.hasPermission`
- `pz.bridge.debug.explainPermission`
- `pz.bridge.debug.listPermissions`

#### Role methods
- `pz.bridge.debug.assignRoleToSelf`
- `pz.bridge.debug.revokeRoleFromSelf`
- `pz.bridge.debug.hasRole`
- `pz.bridge.debug.listRoles`

These are not placeholders. `DebugBridge.register()` directly registers them into `ModuleRegistry` with `BridgeMethodDefinition` metadata and handler lambdas.

---

## Supporting Methods Inside `DebugBridge`

In addition to `register()`, the class currently uses these internal helpers:

- `isLocalIdentityReady()`
- `requireLocalIdentity()`
- `runSmokeSuite(PlayerIdentity identity, String nodeKey, String nodeType)`
- `snapshot(String reason)`

### `isLocalIdentityReady()`
Checks `IdentityLifecycleService.getLastIdentity()` and confirms a non-blank external account id exists.

### `requireLocalIdentity()`
Enforces that a resolved local identity exists before certain bridge methods can run.

### `runSmokeSuite(...)`
Builds a debug result map that currently exercises:
- local snapshot capture
- node claim
- owned node listing
- ownership permission explanation
- negative permission explanation
- direct permission grant
- permission check
- role assignment
- role check
- role listing

This is the most concentrated “prove the whole chain works” method in the module.

### `snapshot(...)`
Builds a lifecycle/debug snapshot map containing:
- `reason`
- `ready`
- lifecycle snapshot data
- auth self-test status
- account id
- player number
- account external id
- full identity map

---

## Current Dependencies and Service Calls

The current bridge surface directly relies on these service classes:
- `AuthSelfTestService`
- `IdentityLifecycleService`
- `OwnershipService`
- `PermissionService`
- `RoleService`

It also relies on:
- `PlayerIdentity`
- `ModuleRegistry`
- `BridgeMethodDefinition`
- `BridgeResult`

So the current bridge module is not abstract. It is already wired into concrete identity, ownership, permission, and role flows.

---

## What This Module Owns

This module owns:
- Java bridge bootstrap
- current method registration
- debug bridge method definitions
- debug/snapshot/smoke test actions
- the Java side of the callable `pz.bridge.debug.*` surface

It does **not** own:
- the runtime registry implementation
- the engine Lua patch
- the mod UI
- the client/server event hooks

Those live in loader, engine, and DBI.

---

## Current Reality Of The Module

The most important thing to say plainly is this:

**the current repo’s live Java bridge surface is the debug bridge surface.**

That is what the code registers today.
That is what Lua calls today.
That is what the mod UI is wired to today.

So the right way to document this module is not “this module may someday do X.”
The right way is:
this module currently boots and registers the `pz.bridge.debug.*` runtime.
