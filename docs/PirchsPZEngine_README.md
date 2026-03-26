# PirchsPZEngine README

## Module Role

`PirchsPZEngine` is the engine patch module.

This module is the repo’s Project Zomboid-facing Java patch layer. It is where the codebase moves out of plain service registration and into engine integration.

The current repo already uses it for one specific and important job:
**patching `zombie.Lua.LuaManager` so PZLife bridge globals are exposed into Lua.**

---

## Build Shape

The module is an active Gradle subproject and currently:
- uses the `java-library` plugin,
- builds the `PirchsPZEngine` jar,
- depends on `project(':PirchsPZBridge')`,
- compiles against `projectzomboid.jar`,
- compiles against other jars in the Project Zomboid root and `lib/` directories.

That means this module is intentionally coupled to the engine classes and is designed to sit closer to Project Zomboid internals than the bridge module does.

---

## Functional Shape

### `zombie.Lua.LuaManager`
The current engine module carries a patched `LuaManager.java`.

Inside `LuaManager.Exposer.exposeAll()`, the PZLife patch currently adds two real integration points:

#### 1. Injector call
```java
PZLifeLuaInjector.expose(this);
```

This is the engine-side hook that allows extra exposure work to run inside the Lua exposer.

#### 2. Global object exposure
```java
this.exposeGlobalFunctions(new PZLifeGlobalObject());
```

This publishes the PZLife global functions into the Lua environment.

The patch also prints explicit startup markers:
- `>>> PZLIFE PATCHED LUAMANAGER INIT <<<`
- `>>> PZLIFE PATCHED EXPOSER EXPOSEALL <<<`
- `>>> PZLIFE GLOBAL OBJECT EXPOSING <<<`

That makes it obvious in logs when the patched engine exposure path ran.

---

## Why This Module Exists

The bridge module registers named Java methods.
The engine module makes those capabilities reachable from the actual Lua runtime surface inside Project Zomboid.

That separation is correct.

`PirchsPZBridge` answers the question:
**what methods exist?**

`PirchsPZEngine` answers the question:
**how do those capabilities get exposed inside the live engine Lua environment?**

---

## What This Module Owns

This module owns:
- Project Zomboid compile-coupled Java code
- the patched `LuaManager`
- PZLife Lua exposure wiring inside `Exposer.exposeAll()`
- the engine-side path that makes PZLife globals available to Lua

It does **not** own:
- agent startup
- registry/dispatcher infrastructure
- debug method registration
- mod UI event handling

Those belong to Agent, Loader, Bridge, and DBI.

---

## Why This Module Matters

Without this module, the Java bridge could still exist internally but Lua would not get the patched exposure path that the current repo relies on.

This module is the seam between:
- registered Java bridge behavior
- and usable Lua globals inside Project Zomboid

That makes it one of the most important layers in the current repo.

---

## Current Reality Of The Module

The current state of `PirchsPZEngine` is not a placeholder. It already performs a concrete engine patch.

That patch is:
- inject additional PZLife exposure work
- expose a PZLife global object
- do it inside `LuaManager.Exposer.exposeAll()`

That is the module’s current functional shape.
