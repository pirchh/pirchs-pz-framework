# PirchsPZBridge README

## Module Role

`PirchsPZBridge` is the Java bridge module.

In the current repo state, this module is where the framework’s Java-side callable surface is registered and where several important runtime/lifecycle behaviors live.

This module is already functional. It is not a placeholder service layer.

Right now it owns the framework’s live bridge/debug surface, identity lifecycle handoff points, and Java-side identity detection support.

---

## Module Scope In Plain English

This module currently answers three different questions:

1. **What bridge methods exist?**
2. **What Java services do those methods call?**
3. **How does the Java side react when local identity becomes available?**

That makes it one of the most important layers in the repo today.

---

## Functional Shape

### `pirch.pz.BridgeBootstrap`

`BridgeBootstrap` is the current Java bootstrap target loaded by the agent.

Its job is to initialize the bridge layer after the agent and loader have done their work.

It currently defines:

- `initialize()`
- `isInitialized()`

The intended split is already clear in the repo:

- Java-side services and bridge methods initialize here
- Lua exposure happens later through the engine patch layer

That is a correct separation of concerns.

---

## Current Bridge Surface

### `pirch.pz.bridge.DebugBridge`

`DebugBridge` is the actual current bridge registration class.

It registers the live named method surface that Lua can call today.

This is not hypothetical future API. These methods are the real bridge surface in the current codebase.

---

## Registered Methods

### Identity / lifecycle methods
- `pz.bridge.debug.isLocalIdentityReady`
- `pz.bridge.debug.selfTestNow`
- `pz.bridge.debug.selfTestStatus`
- `pz.bridge.debug.resetLifecycle`
- `pz.bridge.debug.localSnapshot`

### Smoke testing
- `pz.bridge.debug.runSmokeSuite`

### Ownership methods
- `pz.bridge.debug.claimNode`
- `pz.bridge.debug.releaseNode`
- `pz.bridge.debug.getNodeOwner`
- `pz.bridge.debug.listOwnedNodes`

### Permission methods
- `pz.bridge.debug.grantPermissionToSelf`
- `pz.bridge.debug.revokePermissionFromSelf`
- `pz.bridge.debug.hasPermission`
- `pz.bridge.debug.explainPermission`
- `pz.bridge.debug.listPermissions`

### Role methods
- `pz.bridge.debug.assignRoleToSelf`
- `pz.bridge.debug.revokeRoleFromSelf`
- `pz.bridge.debug.hasRole`
- `pz.bridge.debug.listRoles`

### Runtime diagnostics
- `pz.bridge.debug.listAvailableMethods`
- `pz.bridge.debug.bridgeSnapshot`

These are all currently registered into `ModuleRegistry` with `BridgeMethodDefinition` metadata and handler lambdas.

---

## What `DebugBridge` Actually Does

`DebugBridge.register()` is doing real framework work right now.

It is not just mapping buttons to console prints.

It already wires the bridge to actual Java-side services such as:

- `AuthSelfTestService`
- `IdentityLifecycleService`
- `OwnershipService`
- `PermissionService`
- `RoleService`

This means the bridge layer is already sitting on top of concrete service logic.

That is a major milestone because it proves the framework’s bridge contract can already drive meaningful Java-owned state.

---

## Supporting Methods Inside `DebugBridge`

In addition to `register()`, the class currently uses several important helpers.

### `isLocalIdentityReady()`
Checks whether the current resolved local identity exists and has a non-blank external account id.

This is the guard that tells the rest of the debug/runtime surface whether bridge actions that depend on local identity are currently safe to run.

---

### `requireLocalIdentity()`
Enforces that a resolved local identity exists before certain actions can execute.

This is one of the most important pieces of discipline in the current bridge layer.

It prevents downstream bridge actions from pretending they can operate without a known local identity context.

---

### `runSmokeSuite(...)`
Builds a structured result map that currently exercises:

- local snapshot capture
- node claim
- owned node listing
- ownership permission explanation
- a negative permission explanation
- direct permission grant
- permission check
- role assignment
- role check
- role listing

This is one of the most concentrated “prove the whole chain works” methods in the module.

It demonstrates:
- bridge invocation,
- identity dependency,
- service calls,
- structured results,
- and runtime verification.

---

### `snapshot(String reason)`
Builds a lifecycle/debug snapshot map containing:

- `reason`
- `ready`
- lifecycle snapshot data
- auth self-test status
- account id
- player number
- account external id
- full identity map

This is extremely useful for runtime debugging and is also a sign that the framework is already introspectable, not opaque.

---

### `bridgeSnapshot()`
Builds a bridge/runtime diagnostic map containing:

- whether the bridge was registered
- loader snapshot data
- method count
- method definitions
- metadata for every registered bridge method

This is a valuable addition because it lets the in-game Lua side ask the Java bridge what it actually thinks is loaded and available.

That is good framework behavior.

---

## Identity Lifecycle Hooks

### `pirch.pz.debug.IdentityLifecycleBridge`

This class is the Java-side lifecycle handoff point used by the Lua readiness flow.

It currently provides:

- `markReady()`
- `isReady()`
- `onLocalPlayerCreated(int playerNum, IsoPlayer player)`
- `onServerPlayerReady(IsoPlayer player, String module, String command)`

Both meaningful entry points route into a shared private resolver that calls:

- `IdentityLifecycleService.resolveAndPromoteLocalPlayer(...)`

Once identity promotion succeeds, the class logs the promoted identity data.

That means identity lifecycle resolution is not trapped in Lua. It is already Java-owned.

---

## Java-Side Identity Discovery

### `pirch.pz.debug.IdentityDiscoveryWatcher`

This class gives the bridge module a Java-side detector path for local player discovery.

It currently:

- starts a scheduled executor
- respects runtime configuration
- polls on an interval
- counts visible players
- attempts to resolve any available `IsoPlayer`
- calls into `IdentityLifecycleBridge.onLocalPlayerCreated(...)`
- monitors resolved sessions
- rearms lifecycle detection when a resolved session disappears

This is a big step forward because it means the framework now has lifecycle monitoring behavior that does not depend purely on one Lua event path.

It is more robust and more framework-like.

---

## Why The Bridge Module Matters

The bridge module is where the framework stops being “Java code in the process” and becomes “Java capabilities that Lua and game-facing systems can actually call.”

`PirchsPZLoader` gives the framework a runtime contract.

`PirchsPZBridge` gives that contract a real method surface.

That is a critical distinction.

---

## Current Dependencies

The module currently has a PostgreSQL dependency and also depends on `PirchsPZLoader`.

That aligns with the current direction of the repo:

- Java-owned persistence-related work
- service-backed bridge methods
- named callable runtime surface

This module is already positioned as the place where persistence-aware services meet bridge invocation.

---

## What This Module Owns

This module owns:

- Java bridge bootstrap
- method registration
- debug/runtime method definitions
- identity lifecycle handoff entry points
- Java-side identity detection support
- service-backed bridge calls
- snapshots and smoke-test flows

It does **not** own:

- JVM startup
- the loader runtime internals
- the engine Lua exposure patch
- the in-game UI
- the client/server Lua event wiring

Those belong to Agent, Loader, Engine, and the mod package.

---

## Current Reality Of The Module

The most important thing to say plainly is this:

**the current repo’s live Java bridge surface is no longer just a minimal proof-of-concept.**

It already exposes:
- identity readiness,
- lifecycle reset/snapshot,
- ownership actions,
- permission actions,
- role actions,
- method listing,
- and runtime snapshots.

So the right way to document this module is:

> this module currently boots and registers the framework’s live Java bridge runtime.

That is what it does today.
