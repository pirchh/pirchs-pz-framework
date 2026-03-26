# Pirchs PZ DBI

<div align="center">

# Pirchs PZ DBI
### Java-side runtime, engine patch layer, Lua bridge, and debug identity tooling for Project Zomboid

> Pirchs PZ DBI is a multi-module Project Zomboid framework that boots custom Java code through a Java agent, installs a bridge runtime for named method dispatch, patches the engine Lua exposure layer, and ships a mod package that drives identity readiness and in-game debug operations.

![Java](https://img.shields.io/badge/Java-Active-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Runtime](https://img.shields.io/badge/Runtime-Java%20Agent-blue?style=for-the-badge)
![Engine](https://img.shields.io/badge/Engine-LuaManager%20Patch-5B8CFF?style=for-the-badge)
![Bridge](https://img.shields.io/badge/Bridge-Lua%20%E2%86%94%20Java-purple?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Repo%20Wired-orange?style=for-the-badge)

</div>

---

## Overview

Pirchs PZ DBI is currently built as a five-part stack:

1. **PirchsPZAgent** starts first and loads jars from the mod `lib` folder.
2. **PirchsPZLoader** provides the shared runtime contract for method definitions, requests, results, registration, and dispatch.
3. **PirchsPZBridge** registers the Java-side bridge surface.
4. **PirchsPZEngine** patches the Project Zomboid Lua engine exposure layer so PZLife globals can be exposed inside Lua.
5. **PirchsPZDBI/42** is the actual mod package that loads in game, sends readiness signals, and provides the debug console.

That is the real current shape of the repository. This repo is not a generic “future framework” document set. It is a live stack with a defined startup path and a concrete debug bridge surface.

---

## Current Functional Shape

### Java agent boot
The runtime starts through `PirchsPZAgent`, whose `AgentEntry` class:
- accepts `premain(...)`,
- parses semicolon-delimited agent args,
- resolves the mod lib path,
- resolves the bootstrap class,
- loads jars from the lib folder,
- and invokes `initialize()` on the bootstrap class.

### Runtime contract
The loader layer implements the bridge runtime primitives:
- `LoaderBootstrap.initialize()` and `isInitialized()`
- `BridgeRequest.of(...)`
- `BridgeResult.ok(...)` and `BridgeResult.fail(...)`
- `ModuleRegistry.register(...)`, `get(...)`, `getDefinition(...)`, `has(...)`, `count()`, and `getAllDefinitions()`
- `InvocationDispatcher.invoke(...)`

### Registered bridge surface
The bridge layer currently boots through `BridgeBootstrap.initialize()` and registers the debug bridge.

The current named bridge methods are:

- `pz.bridge.debug.isLocalIdentityReady`
- `pz.bridge.debug.selfTestNow`
- `pz.bridge.debug.selfTestStatus`
- `pz.bridge.debug.runSmokeSuite`
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

This is the current callable Java-side bridge surface in the repo.

### Engine patch behavior
The engine module patches `zombie.Lua.LuaManager` and adds PZLife code inside `LuaManager.Exposer.exposeAll()`.

That patch currently does two concrete things:
- calls `PZLifeLuaInjector.expose(this)`
- calls `this.exposeGlobalFunctions(new PZLifeGlobalObject())`

That is the path that makes the PZLife globals available to Lua.

### Mod package behavior
The DBI mod package currently does three core jobs:
- `PirchsPZDBI_IdentityReady.lua` resolves the local player and sends `PirchsPZDBI / PlayerReady` from clients.
- `PirchsPZDBI_ServerIdentityReady.lua` listens for `PlayerReady` and forwards the event into `pirch.pz.debug.IdentityLifecycleBridge.onServerPlayerReady(...)`.
- `PirchsPZDBI_Debug.lua` creates the in-game debug console and maps UI actions to the registered `pz.bridge.debug.*` methods.

---

## Repository Modules

### `PirchsPZAgent`
The JVM entry module. It owns early startup, lib-folder jar loading, and bootstrap invocation.

### `PirchsPZLoader`
The runtime contract module. It owns method metadata, request/result wrappers, registration, and dispatch.

### `PirchsPZBridge`
The Java bridge module. It currently registers the debug bridge surface and marks the bridge runtime initialized.

### `PirchsPZEngine`
The engine patch module. It compiles against Project Zomboid classes and modifies `LuaManager` so PZLife globals are exposed into Lua.

### `PirchsPZDBI/42`
The actual Project Zomboid mod package. It contains `mod.info`, Lua client/server scripts, and the debug UI.

---

## Execution Flow

```text
Project Zomboid launch
    ->
-javaagent:PirchsPZAgent
    ->
AgentEntry.start(...)
    ->
ModJarLoader.loadModLibFolder(...)
    ->
pirch.pz.BridgeBootstrap.initialize()
    ->
DebugBridge.register()
    ->
Patched LuaManager.Exposer.exposeAll()
    ->
PZLife globals become available in Lua
    ->
PirchsPZDBI client/server Lua scripts run
    ->
Identity readiness + debug actions drive bridge calls
```

---

## Bridge Runtime Model

The bridge runtime is explicit and simple:

- bridge methods are described with `BridgeMethodDefinition`
- method calls are wrapped in `BridgeRequest`
- methods are stored in `ModuleRegistry`
- calls are executed through `InvocationDispatcher`
- return values are normalized as `BridgeResult`

That gives the repo a stable callable surface instead of ad hoc static calls.

---

## In-Game Debug Surface

The current debug console is organized into five tabs:

- Identity
- Nodes
- Permissions
- Roles
- Utility

The UI maps button clicks directly to the Java bridge methods listed above. That makes the current repo especially strong for live manual testing of:
- local identity readiness
- smoke tests
- node ownership actions
- permission grant/revoke/check/explain flows
- role assign/revoke/check/list flows
- bridge status checks

---

## Repo Layout

```text
pirchs-pz-dbi/
├─ PirchsPZAgent/
├─ PirchsPZLoader/
├─ PirchsPZBridge/
├─ PirchsPZEngine/
├─ PirchsPZDBI/
│  └─ 42/
├─ gradle/
├─ README.md
├─ build.gradle
├─ settings.gradle
├─ gradlew
└─ gradlew.bat
```

---

## Docs Included in This Package

This package includes:

- `README.md`
- `docs/PirchsPZAgent_README.md`
- `docs/PirchsPZLoader_README.md`
- `docs/PirchsPZBridge_README.md`
- `docs/PirchsPZEngine_README.md`
- `docs/PirchsPZDBI_README.md`

The root README explains the whole stack.
Each module README goes deeper and names the exact methods, files, and responsibilities currently present in the repo.

---

