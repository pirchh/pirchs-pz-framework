# PirchsPZEngine README

## Module Role

`PirchsPZEngine` is the engine patch module.

This module is the Project Zomboid-facing Java patch layer of the framework. It is where the codebase moves out of service registration and into engine integration.

The current repo already uses it for one specific and important job:

**patching `zombie.Lua.LuaManager` so the framework’s Lua exposure path is available in-game.**

That makes this module one of the most important seams in the entire stack.

---

## Build Shape

The module is an active Gradle subproject and currently:

- uses the `java-library` plugin,
- builds the `PirchsPZEngine` jar,
- depends on `project(':PirchsPZBridge')`,
- compiles against `projectzomboid.jar`,
- compiles against other jars in the Project Zomboid root and `lib/` directories.

That means this module is intentionally compile-coupled to Project Zomboid internals.

That is exactly what you want from the engine patch layer and exactly what you do **not** want to spread across the rest of the repo.

---

## Functional Shape

### Patched `zombie.Lua.LuaManager`

The current engine module carries a patched `LuaManager.java`.

Inside `LuaManager.Exposer.exposeAll()`, the framework patch currently adds two real integration points:

### 1. Injector call

```java
PZLifeLuaInjector.expose(this);
```

This is the engine-side hook that allows the framework’s extra exposure work to run inside the Lua exposer.

---

### 2. Global object exposure

```java
this.exposeGlobalFunctions(new PZLifeGlobalObject());
```

This publishes the framework’s global functions into the Lua environment.

Together, these lines are the bridge between:
- Java runtime registration
- and usable Lua-side access in the live engine

---

## Log Markers

The patch also prints explicit startup markers such as:

- `>>> PZLIFE PATCHED LUAMANAGER INIT <<<`
- `>>> PZLIFE PATCHED EXPOSER EXPOSEALL <<<`
- `>>> PZLIFE GLOBAL OBJECT EXPOSING <<<`

That is valuable because engine patching is one of the places where you most need obvious logging to know whether your modified path actually executed.

---

## Why This Module Exists

The bridge module registers named Java methods.

The engine module makes those capabilities reachable from the actual Lua runtime surface inside Project Zomboid.

That separation is correct.

`PirchsPZBridge` answers:
**what methods exist?**

`PirchsPZEngine` answers:
**how do those capabilities become reachable inside the live engine Lua environment?**

That is the exact job of this module.

---

## Why This Split Is Healthy

Keeping engine coupling isolated inside this module is a strong architectural choice.

It prevents the rest of the framework from becoming polluted with direct engine-patch concerns.

That means:
- Agent can stay focused on startup
- Loader can stay focused on runtime contracts
- Bridge can stay focused on method registration and service wiring
- DBI/mod package can stay focused on in-game scripts and UI

The engine patch layer should be the narrow seam between the framework and Project Zomboid internals.

That is what this module is already doing.

---

## What This Module Owns

This module owns:

- Project Zomboid compile-coupled Java code
- the patched `LuaManager`
- framework exposure wiring inside `Exposer.exposeAll()`
- the engine-side path that makes framework globals available to Lua

It does **not** own:

- agent startup
- loader registration/dispatch internals
- bridge method registration
- mod UI behavior
- client/server readiness events

Those belong to Agent, Loader, Bridge, and the in-game mod package.

---

## Current Reality Of The Module

The current state of `PirchsPZEngine` is not a placeholder.

It already performs a concrete engine patch.

That patch currently:
- injects extra framework exposure work,
- exposes a framework global object,
- does it inside `LuaManager.Exposer.exposeAll()`.

That is the module’s actual functional shape right now.

The right way to document it is simply:

> this module is the engine seam that exposes the framework into Lua.
