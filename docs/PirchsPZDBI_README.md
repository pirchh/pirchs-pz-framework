# PirchsPZDBI README

## Module Role

`PirchsPZDBI/42` is the actual Project Zomboid mod package.

Even though the repository has moved to the broader **Pirchs PZ Framework** identity, this package path still matters because it is what the game loads as the active mod package.

This is the in-game layer of the framework. It contains the mod metadata, the Lua lifecycle hooks, the client/server readiness wiring, and the debug console that exercises the Java bridge.

This is not a passive shell. It is the current game-facing interaction layer of the framework.

---

## Mod Metadata

The current `mod.info` declares:

- `name=PirchsPZDBI`
- `id=pirchs_pz_dbi`
- `description=Server-side DB + identity bridge for PZLife`
- `poster=poster.png`

That metadata still reflects the older internal package naming, which is acceptable while the repo identity broadens into the framework name.

The important thing is that the package’s runtime role is already clear:
this mod is the in-game face of the Java bridge and identity lifecycle stack.

---

## Functional Shape

This package currently does three major jobs:

1. it tells Project Zomboid what the mod package is,
2. it drives the Lua-side readiness/lifecycle handoff into Java,
3. it provides the in-game debug console for the current bridge surface.

That is the correct way to think about the package.

---

## Client Readiness Script

### `media/lua/client/PirchsPZDBI_IdentityReady.lua`

This client-side script handles local readiness signaling.

It currently does the following:

1. logs that it loaded,
2. tracks whether resolution has already happened with `resolved`,
3. defines `resolve(player)`,
4. gets the local player when none is passed,
5. reads `playerNum`,
6. logs the resolve attempt,
7. sends `sendClientCommand("PirchsPZDBI", "PlayerReady", { playerNum = playerNum })` when running as a client,
8. otherwise logs that single-player relies on the Java-side detector,
9. registers the handler with `Events.OnPlayerUpdate.Add(resolve)`.

This file is the current client-to-server readiness handoff for identity.

That is already a real framework lifecycle step.

---

## Server Readiness Forwarder

### `media/lua/server/PirchsPZDBI_ServerIdentityReady.lua`

This server-side script handles the matching server event.

It currently:

1. logs that it loaded,
2. defines `onClientCommand(module, command, player, args)`,
3. ignores commands that are not `PirchsPZDBI / PlayerReady`,
4. logs that `PlayerReady` was received,
5. calls `pirch.pz.debug.IdentityLifecycleBridge.onServerPlayerReady(player, module, command)` when a player is present,
6. registers the listener with `Events.OnClientCommand.Add(onClientCommand)`.

That means the current server script is not generic event glue. It is a very specific identity lifecycle bridge handoff path.

---

## Debug Console

### `media/lua/client/PirchsPZDBI_Debug.lua`

This is the in-game debug console.

It currently:

- requires `ISPanel`, `ISButton`, and `ISTextEntryBox`
- binds the console hotkey to `F6`
- creates a panel-derived UI object
- keeps an activity log and last-result area
- tracks bridge status
- provides multiple input fields
- provides runtime diagnostics
- maps UI actions to named Java bridge methods

The current five tabs are:

- `Identity`
- `Nodes`
- `Permissions`
- `Roles`
- `Utility`

That is a meaningful framework-debug UI, not a placeholder window.

---

## Current Method Map

The current debug menu maps actions to the following bridge methods:

- `READY -> pz.bridge.debug.isLocalIdentityReady`
- `SNAPSHOT -> pz.bridge.debug.localSnapshot`
- `SELF_TEST_NOW -> pz.bridge.debug.selfTestNow`
- `SELF_TEST_STATUS -> pz.bridge.debug.selfTestStatus`
- `RESET_LIFECYCLE -> pz.bridge.debug.resetLifecycle`
- `RUN_SMOKE -> pz.bridge.debug.runSmokeSuite`
- `CLAIM_NODE -> pz.bridge.debug.claimNode`
- `RELEASE_NODE -> pz.bridge.debug.releaseNode`
- `GET_NODE_OWNER -> pz.bridge.debug.getNodeOwner`
- `LIST_OWNED_NODES -> pz.bridge.debug.listOwnedNodes`
- `GRANT_PERMISSION -> pz.bridge.debug.grantPermissionToSelf`
- `REVOKE_PERMISSION -> pz.bridge.debug.revokePermissionFromSelf`
- `HAS_PERMISSION -> pz.bridge.debug.hasPermission`
- `EXPLAIN_PERMISSION -> pz.bridge.debug.explainPermission`
- `LIST_PERMISSIONS -> pz.bridge.debug.listPermissions`
- `ASSIGN_ROLE -> pz.bridge.debug.assignRoleToSelf`
- `REVOKE_ROLE -> pz.bridge.debug.revokeRoleFromSelf`
- `HAS_ROLE -> pz.bridge.debug.hasRole`
- `LIST_ROLES -> pz.bridge.debug.listRoles`
- `LIST_METHODS -> pz.bridge.debug.listAvailableMethods`
- `BRIDGE_SNAPSHOT -> pz.bridge.debug.bridgeSnapshot`

These last two runtime-diagnostic actions are especially important because they show the Lua UI is already aware of framework introspection and runtime method discovery, not just feature actions.

---

## Bridge Availability Behavior In Lua

The debug menu’s current bridge logic is also worth documenting correctly.

It checks the canonical `pz.bridge` contract and uses:

- `pz.bridge.isAvailable()`
- `pz.bridge.status()`
- `pz.bridge.invokeIfPresent(...)`

If the canonical API is not available, the menu also captures fallback diagnostic information around legacy global exposure state such as:

- `pzlife`
- `pzlife_has`
- `pzlife_invoke0`
- `pzlife_invoke1`
- `pzlife_invoke2`
- `pzlife_invoke3`

That means the debug panel is not only a “call methods” UI. It is also a useful runtime-diagnostics tool while the framework continues to mature.

---

## Inputs Exposed By The Debug UI

The current debug menu provides text inputs for:

- node key
- node type
- permission
- role

These are used to drive ownership/permission/role operations directly from the in-game UI.

That makes the package useful for live manual verification of service-backed Java methods.

---

## Result And Log Behavior

The UI keeps:

- `lastResult`
- wrapped result text
- `statusText`
- status colors
- bridge status snapshots
- supported method tracking
- rolling activity log lines

This matters because it means the debug console is already designed as a runtime inspection tool, not just a button tray.

---

## What This Module Owns

This module owns:

- mod metadata
- client-side readiness signaling
- server-side readiness forwarding
- in-game debug UI
- bridge availability checks in Lua
- method-to-button mapping for the debug bridge
- runtime inspection behavior in the Lua layer

It does **not** own:

- JVM startup
- loader internals
- Java bridge registration
- engine patching
- Java-side service logic

Those belong to Agent, Loader, Bridge, and Engine.

---

## Why This Module Matters

This package is where the framework becomes visible inside the game.

Without it, the framework might still boot, register methods, and patch the engine, but there would be no current in-game lifecycle signal and no current in-game interaction panel to prove the runtime path is working.

Today this package is doing that proof work continuously.

It is a key part of why the framework can now be documented confidently.

---

## Current Reality Of The Module

The current mod package does three real jobs today:

1. it tells the game what the mod is,
2. it sends and receives the `PlayerReady` lifecycle signal,
3. it provides the debug console that exercises the live `pz.bridge.debug.*` surface.

That is the correct way to document this module in its current state.

Even though the repo identity is now broader, this package remains the active game-facing mod layer that makes the framework testable and usable in-game.
