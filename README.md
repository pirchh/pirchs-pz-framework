# Pirchs PZ Framework

<div align="center">

# Pirchs PZ Framework
### Java-authoritative runtime framework, bridge layer, engine patch, and in-game Lua tooling for Project Zomboid

> Pirchs PZ Framework is a multi-module Project Zomboid framework that starts through a Java agent, loads custom jars from the mod `lib` folder, initializes a named Java bridge runtime, patches the engine Lua exposure path, and ships an in-game mod package that drives identity readiness, lifecycle promotion, and live debug interaction.

![Java](https://img.shields.io/badge/Java-Active-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Runtime](https://img.shields.io/badge/Runtime-Java%20Agent-blue?style=for-the-badge)
![Bridge](https://img.shields.io/badge/Bridge-Lua%20%E2%86%94%20Java-purple?style=for-the-badge)
![Engine](https://img.shields.io/badge/Engine-LuaManager%20Patch-5B8CFF?style=for-the-badge)
![Identity](https://img.shields.io/badge/Identity-Resolved-green?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Bootstrapping%20%2B%20Data%20Flow-orange?style=for-the-badge)

</div>

---

## Overview

Pirchs PZ Framework is no longer best described as a narrow “DB bridge” repo.

In its current working shape, this repository is a **Java-authoritative runtime framework for Project Zomboid**. It already has a real startup path, a real bridge contract, a real engine exposure patch, a real in-game Lua package, and a real identity lifecycle handoff that crosses the Lua/Java boundary.

That matters because the repo is now beyond “can we make Java talk to Lua?” territory.

The current stack already proves all of the following:

- custom Java code starts through `-javaagent`
- jars are loaded from the mod `lib` folder
- a bootstrap class is invoked
- the loader and bridge runtime initialize
- Project Zomboid’s Lua exposure path is patched
- Lua gains access to the framework bridge surface
- client Lua signals player readiness
- server Lua forwards that readiness into Java
- Java resolves and promotes local identity
- Lua can call structured Java bridge methods from an in-game debug console

That is the real current state of the codebase.

---

## Repository Identity

The repository has been renamed to **Pirchs PZ Framework**.

That rename is broader and more accurate than the older “DBI” framing because the repo now contains:

- a JVM boot layer
- a runtime contract layer
- a callable named bridge layer
- an engine patch layer
- an in-game mod layer
- identity lifecycle handling
- ownership, permission, and role service wiring
- a debug/runtime diagnostics surface

The folder names and package names inside the repo do not all need to change immediately for the repo identity change to be valid. The current repo shape still includes module/package names like:

- `PirchsPZAgent`
- `PirchsPZLoader`
- `PirchsPZBridge`
- `PirchsPZEngine`
- `PirchsPZDBI/42`

That is fine. The framework naming change is about the project’s overall purpose and direction, not about forcing a disruptive internal rename all at once.

---

## What This Repo Is

This repo is:

- a Project Zomboid framework
- Java-authoritative
- bridge-driven
- engine-patched
- persistence-oriented
- built to support higher-level gameplay systems later

This repo is **not**:

- just a UI mod
- just a proof of concept
- just a raw database experiment
- just a one-off debug tool
- just a loose collection of scripts

The current code already has enough structure to be documented as a framework kernel.

---

## Current Functional Shape

The current repository is best understood as a five-layer stack:

1. **PirchsPZAgent** starts first and controls JVM boot entry.
2. **PirchsPZLoader** defines the runtime contract for named bridge methods.
3. **PirchsPZBridge** registers the current Java bridge surface and lifecycle/debug actions.
4. **PirchsPZEngine** patches Project Zomboid’s Lua exposure path so framework globals are available in Lua.
5. **PirchsPZDBI/42** is the in-game mod package that sends readiness signals and provides the debug console.

That is the current live shape of the repo.

---

## Module Summary

### `PirchsPZAgent`
The Java agent bootstrap jar.

This module owns:
- `premain(...)`
- agent arg parsing
- mod lib path resolution
- jar discovery
- class loader creation
- bootstrap class invocation

This is the layer that gets the custom Java stack into the game process.

---

### `PirchsPZLoader`
The runtime contract module.

This module owns:
- method metadata
- request wrapping
- result wrapping
- registry storage
- invocation dispatch
- loader init state

This is the framework’s structured callable runtime.

---

### `PirchsPZBridge`
The Java bridge/service layer.

This module currently owns:
- bridge bootstrap
- debug bridge registration
- identity lifecycle bridge hooks
- ownership/permission/role bridge wiring
- Java-side smoke testing and snapshots
- Java-side identity discovery watcher

This is the layer Lua calls into once the bridge is available.

---

### `PirchsPZEngine`
The engine patch layer.

This module owns:
- the patched `zombie.Lua.LuaManager`
- framework exposure inside `LuaManager.Exposer.exposeAll()`
- PZLife injector/global-object integration points

This is the layer that makes the Java runtime reachable from Lua in the live engine environment.

---

### `PirchsPZDBI/42`
The actual Project Zomboid mod package.

This module owns:
- `mod.info`
- client readiness signaling
- server readiness forwarding
- the in-game debug console
- bridge availability checks in Lua
- the current button-to-method debug mapping

This is what the game directly loads as the mod package.

---

## Startup Flow

The current startup flow looks like this:

```text
Project Zomboid launch
    ->
-javaagent:PirchsPZAgent
    ->
AgentEntry.start(...)
    ->
ModJarLoader.loadModLibFolder(...)
    ->
bootstrapClass.initialize()
    ->
LoaderBootstrap.initialize()
    ->
BridgeBootstrap.initialize()
    ->
DebugBridge.register()
    ->
patched LuaManager.Exposer.exposeAll()
    ->
PZLife globals + pz.bridge exposure become available in Lua
    ->
client Lua sends PlayerReady
    ->
server Lua forwards PlayerReady into Java
    ->
IdentityLifecycleBridge resolves/promotes local player identity
    ->
debug actions and bridge calls can run against resolved local identity
```

That flow is already materially present in the current repository.

---

## Java Authority Model

The current repo direction is strongest when documented with a clear authority split:

### Java is the source of truth
Java should own:
- identity resolution
- lifecycle promotion
- persistence access
- ownership state
- permission state
- role state
- service rules
- future jobs/professions/business logic

### Lua is the in-game interaction shell
Lua should own:
- UI
- input
- hotkeys
- in-game panels
- event glue
- local debug interaction
- client/server signaling where needed

### The bridge is the contract layer
The bridge should own:
- named methods
- argument validation
- normalized return values
- diagnostics
- method discovery
- safe invocation boundaries

That split matches the current repo and aligns with where the project is clearly heading.

---

## Identity Lifecycle: Current Reality

The current repo now has a real identity path.

### Client-side readiness signal
The client Lua script:

- loads on the client
- waits for a player object
- captures `playerNum`
- sends `sendClientCommand("PirchsPZDBI", "PlayerReady", { playerNum = playerNum })` when running as a client
- falls back to Java-side detection in single-player

This is the current client-side handoff that says: the player exists, the game is ready enough, continue identity work.

---

### Server-side readiness forwarder
The server Lua script:

- listens to `Events.OnClientCommand`
- filters for `PirchsPZDBI / PlayerReady`
- logs that the event was received
- forwards the player into `pirch.pz.debug.IdentityLifecycleBridge.onServerPlayerReady(...)`

That means the server Lua layer is not vague glue; it is a specific Java handoff path.

---

### Java lifecycle bridge
`IdentityLifecycleBridge` currently provides:

- `markReady()`
- `isReady()`
- `onLocalPlayerCreated(int playerNum, IsoPlayer player)`
- `onServerPlayerReady(IsoPlayer player, String module, String command)`

Both of the meaningful entry points route into private `resolve(...)`, which calls:

- `IdentityLifecycleService.resolveAndPromoteLocalPlayer(...)`

Once identity promotion succeeds, the code logs the promoted identity data.

That is exactly the kind of framework behavior that deserves confident documentation. Identity is not merely “planned” now. It is in the actual runtime loop.

---

### Java discovery watcher
The bridge module also includes `IdentityDiscoveryWatcher`, which gives the framework a Java-side detector path.

It currently:
- starts a scheduled executor
- polls at configured intervals
- waits until lifecycle readiness is armed
- attempts to find any visible `IsoPlayer`
- tries to resolve local identity
- keeps itself armed for future session detection
- monitors resolved sessions
- rearms lifecycle detection when the session appears to disappear

This is an important milestone in the repo because it moves the project from “Lua event only” into a broader runtime lifecycle model.

---

## Current Bridge Runtime Model

The bridge runtime is structured, not ad hoc.

### Method definitions
Methods are described with `BridgeMethodDefinition`, which currently carries:
- method name
- version
- description
- minimum argument count

### Requests
Calls are normalized through `BridgeRequest`.

### Results
Return values are normalized through `BridgeResult`.

### Registry
Methods are registered into `ModuleRegistry`.

### Dispatch
Calls are executed through `InvocationDispatcher`.

This gives the framework:
- named methods
- discoverable metadata
- consistent result shapes
- centralized validation
- centralized invocation

That is a major strength of the current architecture.

---

## Current Java Bridge Surface

The current registered debug/runtime bridge methods are:

### Identity / lifecycle
- `pz.bridge.debug.isLocalIdentityReady`
- `pz.bridge.debug.selfTestNow`
- `pz.bridge.debug.selfTestStatus`
- `pz.bridge.debug.runSmokeSuite`
- `pz.bridge.debug.resetLifecycle`
- `pz.bridge.debug.localSnapshot`

### Smoke testing
- `pz.bridge.debug.runSmokeSuite`

### Ownership
- `pz.bridge.debug.claimNode`
- `pz.bridge.debug.releaseNode`
- `pz.bridge.debug.getNodeOwner`
- `pz.bridge.debug.listOwnedNodes`

### Permissions
- `pz.bridge.debug.grantPermissionToSelf`
- `pz.bridge.debug.revokePermissionFromSelf`
- `pz.bridge.debug.hasPermission`
- `pz.bridge.debug.explainPermission`
- `pz.bridge.debug.listPermissions`

### Roles
- `pz.bridge.debug.assignRoleToSelf`
- `pz.bridge.debug.revokeRoleFromSelf`
- `pz.bridge.debug.hasRole`
- `pz.bridge.debug.listRoles`

### Runtime diagnostics
- `pz.bridge.debug.listAvailableMethods`
- `pz.bridge.debug.bridgeSnapshot`

This is the current callable Java-side surface in the repo today.

---

## What `DebugBridge` Actually Proves

`DebugBridge` is currently more than “temporary debug code.”

It proves that the framework can already support a structured end-to-end vertical slice:

- a Java bridge method is registered
- Lua can discover or call it
- the method can require resolved identity
- the method can call service-layer code
- the method can return a structured result
- the result can be displayed back in-game

That matters because this same pattern is what future framework systems will use.

Today it is:
- identity
- ownership
- permissions
- roles
- snapshots
- smoke tests

Tomorrow the same pattern can support:
- professions
- jobs
- business actions
- crafting systems
- trucking contracts
- world interactions
- character systems

That is why the framework naming direction is correct.

---

## Engine Patch Behavior

`PirchsPZEngine` is not a placeholder subproject. It is already doing concrete engine-facing work.

The patched `zombie.Lua.LuaManager` currently adds two meaningful framework calls inside `LuaManager.Exposer.exposeAll()`:

```java
PZLifeLuaInjector.expose(this);
this.exposeGlobalFunctions(new PZLifeGlobalObject());
```

That is the seam between the Java bridge/runtime and the live Lua environment inside Project Zomboid.

The patch also emits obvious startup markers so you can tell in logs when the patched path executed.

That is exactly the right role for this module.

---

## In-Game Lua Debug Surface

The current debug console is no longer just “does the bridge exist?” UI.

It is a real runtime interaction panel with the following tabs:

- `Identity`
- `Nodes`
- `Permissions`
- `Roles`
- `Utility`

The menu currently:
- opens with `F6`
- uses the canonical `pz.bridge` contract when available
- falls back to legacy diagnostics in its status snapshot
- maps actions to named Java methods
- keeps a local activity log
- displays the last structured result
- exposes text inputs for node key, node type, permission, and role
- supports runtime diagnostics like method listing and bridge snapshots

That makes the current Lua package useful for live manual testing and framework diagnostics, not just cosmetic debugging.

---

## Current Persistence Framing

The bridge module already depends on PostgreSQL and already routes through service-layer concepts like:

- ownership
- permissions
- roles
- auth self-tests
- identity lifecycle

That means the persistence-oriented direction of the repo is real.

The current framework is not fully documented yet at the SQL-file level in this README, but the module shape clearly supports persistence-backed systems and already treats identity/authorization/state as Java-owned concerns.

This is enough to document the repo confidently as a persistence-oriented framework.

---

## Why The Old README Framing Needed To Change

The earlier repo framing leaned heavily on “DBI” language.

That made sense when the center of gravity was:
- proving Java startup
- proving bridge access
- proving identity work
- proving service calls

But the current codebase now clearly spans more than that.

The repository already contains:
- a startup system
- a runtime contract
- a bridge registry
- an engine patch
- a mod package
- lifecycle detection
- diagnostics
- service-backed bridge actions

So the right documentation stance now is:

> this repo is a framework kernel with a working identity/debug/service slice already implemented

That is the most accurate way to describe it.

---

## What This Repo Owns Right Now

This framework currently owns:

- JVM entry through a Java agent
- jar loading from the mod `lib` folder
- bootstrap invocation
- runtime contract + dispatch
- method registration + metadata
- Project Zomboid Lua exposure patching
- client/server readiness signaling
- Java-side identity promotion
- Java-side identity discovery monitoring
- ownership bridge actions
- permission bridge actions
- role bridge actions
- self-test and snapshot diagnostics
- in-game debug interaction

That is already a substantial foundation.

---

## What This Repo Does Not Need To Pretend Yet

The framework does **not** need to claim that all future gameplay systems already exist.

The strongest documentation is honest documentation.

So while the framework is clearly being built to support systems like:
- professions
- skills
- businesses
- trucking
- drugs
- economy
- character progression

the current repo should document those as direction, not as present-day completed features.

That keeps the README credible.

---

## Recommended Next Direction

The best next steps for the codebase are no longer “make Java and Lua talk.”

That problem is already materially solved.

The next steps should be:

### 1. Formalize the framework identity
Treat the project as a framework kernel.

### 2. Keep Java authoritative
Do not let future gameplay logic drift back into Lua unless it is truly presentation/input glue.

### 3. Expand the bridge surface intentionally
Add domain-driven methods, not random helpers.

### 4. Define the next real subsystem
The strongest candidates are:
- character/profile system
- professions/skills
- jobs/contracts
- businesses/ownership expansion

### 5. Keep the docs synchronized with the live bridge surface
The bridge method list and module docs should stay aligned with the actual code.

The UI maps button clicks directly to the Java bridge methods listed above. That makes the current repo especially strong for live manual testing of:
- local identity readiness
- smoke tests
- node ownership actions
- permission grant/revoke/check/explain flows
- role assign/revoke/check/list flows
- bridge status checks

## Repository Layout

```text
pirchs-pz-framework/
├─ PirchsPZAgent/
├─ PirchsPZLoader/
├─ PirchsPZBridge/
├─ PirchsPZEngine/
├─ PirchsPZDBI/
│  └─ 42/
├─ docs/
│  ├─ PirchsPZAgent_README.md
│  ├─ PirchsPZLoader_README.md
│  ├─ PirchsPZBridge_README.md
│  ├─ PirchsPZEngine_README.md
│  └─ PirchsPZDBI_README.md
├─ gradle/
├─ README.md
├─ build.gradle
├─ settings.gradle
├─ gradlew
└─ gradlew.bat
```

The current `settings.gradle` includes these active subprojects:

- `PirchsPZLoader`
- `PirchsPZBridge`
- `PirchsPZAgent`
- `PirchsPZEngine`

That matches the current multi-module framework structure.

---

## Docs Included In This Package

This docs refresh includes:

- `README.md`
- `docs/PirchsPZAgent_README.md`
- `docs/PirchsPZLoader_README.md`
- `docs/PirchsPZBridge_README.md`
- `docs/PirchsPZEngine_README.md`
- `docs/PirchsPZDBI_README.md`

The goal of this refresh is not to invent future architecture.

The goal is to make the documentation match what the repo is **already doing now**, while also framing the project correctly as **Pirchs PZ Framework**.

---

## Final Summary

Pirchs PZ Framework is currently a working Java-authoritative Project Zomboid framework stack with:

- agent startup
- structured runtime dispatch
- bridge method registration
- engine Lua exposure patching
- Lua readiness signaling
- Java identity lifecycle promotion
- ownership/permission/role bridge wiring
- live in-game debug diagnostics


