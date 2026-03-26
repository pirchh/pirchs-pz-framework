# PirchsPZDBI README

## Module Role

`PirchsPZDBI/42` is the actual Project Zomboid mod package.

This is the in-game layer of the repo. It contains the mod metadata, the Lua lifecycle hooks, the client/server readiness wiring, and the debug console that exercises the Java bridge.

This is what Project Zomboid loads as the mod.

---

## Mod Metadata

The current `mod.info` declares:

- `name=PirchsPZDBI`
- `id=pirchs_pz_dbi`
- `description=Server-side DB + identity bridge for PZLife`
- `poster=poster.png`

That description is accurate to the current package role: this mod is the in-game face of the identity/bridge stack.

---

## Functional Shape

### `media/lua/client/PirchsPZDBI_IdentityReady.lua`
This client-side script handles local readiness signaling.

It currently does the following:
1. logs that it loaded,
2. tracks whether resolution has already happened with `resolved`,
3. defines `resolve(player)`,
4. gets the local player,
5. reads `playerNum`,
6. logs the resolve attempt,
7. sends `sendClientCommand("PirchsPZDBI", "PlayerReady", { playerNum = playerNum })` when running as a client,
8. otherwise logs that single-player relies on the Java-side detector,
9. registers the handler with `Events.OnPlayerUpdate.Add(resolve)`.

This file is the current client-to-server readiness handoff.

---

### `media/lua/server/PirchsPZDBI_ServerIdentityReady.lua`
This server-side script handles the matching server event.

It currently:
1. logs that it loaded,
2. defines `onClientCommand(module, command, player, args)`,
3. ignores commands that are not `PirchsPZDBI / PlayerReady`,
4. logs that `PlayerReady` was received,
5. calls `pirch.pz.debug.IdentityLifecycleBridge.onServerPlayerReady(player, module, command)` when a player is present,
6. registers the listener with `Events.OnClientCommand.Add(onClientCommand)`.

That means the current server script is not generic glue. It is a specific identity lifecycle handoff path.

---

### `media/lua/client/PirchsPZDBI_Debug.lua`
This is the in-game debug console.

It currently:
- loads `ISPanel`, `ISButton`, and `ISTextEntryBox`
- binds the console hotkey to `F6`
- defines five tabs:
  - `Identity`
  - `Nodes`
  - `Permissions`
  - `Roles`
  - `Utility`
- maps button ids to Java bridge methods through the `METHOD` table
- checks whether the global bridge is present through:
  - `pzlife`
  - `pzlife_has`
  - `pzlife_invoke0`
  - `pzlife_invoke1`
  - `pzlife_invoke2`
  - `pzlife_invoke3`
- routes button clicks through `invokeBridgeMethod(...)` and `invokeById(...)`

The current method map is:

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

The debug menu also exposes text inputs for:
- node key
- node type
- permission
- role

So the current DBI mod package is not just a loader shell. It is a working in-game debug client for the live Java bridge.

---

## What This Module Owns

This module owns:
- mod metadata
- client-side readiness signaling
- server-side readiness forwarding
- in-game debug UI
- bridge availability checks in Lua
- method-to-button mapping for the debug bridge

It does **not** own:
- JVM startup
- registry internals
- Java bridge registration
- engine patching

Those belong to Agent, Loader, Bridge, and Engine.

---

## Current Reality Of The Module

The current mod package does three real jobs today:
1. it tells the game what the mod is,
2. it sends and receives the `PlayerReady` lifecycle signal,
3. it provides the debug console that exercises the `pz.bridge.debug.*` bridge surface.

That is the correct way to document this module in its current state.
