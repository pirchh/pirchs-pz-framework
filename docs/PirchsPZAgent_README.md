# PirchsPZAgent README

## Module Role

`PirchsPZAgent` is the JVM boot module for the framework.

This module starts the entire stack. Its job is not “future startup support.” It is the actual live entry point that gets the custom Java framework code into the Project Zomboid process.

The module takes control at JVM premain time, resolves the mod jar location, loads the jars in the mod `lib` folder, and invokes the configured bootstrap class.

That is its current real responsibility.

---

## Functional Shape

### `pirch.pzagent.AgentEntry`

`AgentEntry` is the agent entry class and implements the framework boot sequence.

It currently defines:

- `premain(String agentArgs, Instrumentation inst)`
- `premain(String agentArgs)`
- `start(String agentArgs)`
- `resolveModLibPath(Map<String, String> parsedArgs)`
- `parseAgentArgs(String agentArgs)`

These methods are the actual top of the runtime stack.

---

## `premain(...)`

Both `premain` overloads route directly into `start(...)`.

That means the framework treats `start(...)` as the single meaningful boot path. This is a good design choice because it keeps startup logic centralized instead of duplicating behavior across agent entry variants.

---

## `start(...)`

`start(...)` performs the actual boot work:

1. logs startup,
2. parses agent args,
3. resolves the mod lib path,
4. resolves the bootstrap class name,
5. loads the mod jars with `ModJarLoader.loadModLibFolder(...)`,
6. loads the bootstrap class with the mod class loader,
7. resolves the `initialize()` method,
8. invokes it.

That makes this class responsible for framework startup handoff, not framework logic itself.

---

## Default Values

The current defaults are:

- default mod lib path: `C:/Users/ryanj/Zomboid/mods/PirchsPZDBI/42/lib`
- default bootstrap class: `pirch.pz.BridgeBootstrap`

That is an important detail because it shows the framework is currently designed to boot through the mod package’s `lib` folder while leaving the in-game package name/path stable.

The repo name can evolve independently from that internal boot path.

---

## `resolveModLibPath(...)`

This method resolves the mod lib path using a concrete fallback order:

1. `modLib` from agent args
2. `pirch.pz.modLib` system property
3. `PIRCHS_PZ_MOD_LIB` environment variable
4. built-in default path

This is a strong, practical design because it supports multiple launch styles without code edits.

It allows you to:
- hardcode nothing during normal operation,
- override behavior in local dev,
- keep the runtime flexible for packaging/testing,
- and preserve a deterministic default when nothing is configured.

---

## `parseAgentArgs(...)`

This method parses semicolon-delimited `key=value` pairs, such as:

```text
modLib=C:/Users/ryanj/Zomboid/mods/PirchsPZDBI/42/lib;bootstrapClass=pirch.pz.BridgeBootstrap
```

It stores them in insertion order using `LinkedHashMap`.

That matters because it means the boot configuration is explicit, simple, and debuggable.

---

## Jar Loading

### `pirch.pzagent.ModJarLoader`

`ModJarLoader` owns jar discovery and class loader creation.

It currently defines:

- `loadModLibFolder(Path libFolder)`

That method:

1. validates the folder exists,
2. lists files in the folder,
3. filters for `.jar`,
4. sorts the jar paths,
5. logs each discovered jar,
6. converts them to `URL`s,
7. creates a `URLClassLoader` backed by the system class loader.

This is the core of “framework jars live in the mod lib folder and are pulled into the game process.”

---

## Failure Behavior

If the folder is missing or contains no jars, the loader fails immediately.

That is the correct behavior for this module.

Silent partial startup would make runtime debugging dramatically worse. The agent layer is strongest when it either:
- boots cleanly,
- or fails loudly.

The current shape follows that rule.

---

## Logging

### `pirch.pzagent.AgentLog`

The agent module includes dedicated logging helpers.

That matters because agent startup is one of the most failure-prone phases in the framework. This logging is what tells you:

- startup began,
- which mod lib path was chosen,
- which bootstrap class was chosen,
- which jars were found,
- whether bootstrap invocation succeeded,
- and where boot failed if it did fail.

The agent layer should always be noisy enough to debug startup. The current module reflects that.

---

## Build Shape

The module jar is explicitly configured with:

- `archiveBaseName = 'PirchsPZAgent'`
- `Premain-Class = pirch.pzagent.AgentEntry`
- `Can-Redefine-Classes = false`
- `Can-Retransform-Classes = false`

That tells you exactly what this jar is:

- a Java agent bootstrap jar
- not a generalized instrumentation toolkit
- not a class-redefinition platform
- not a bytecode mutation framework

It is a narrow startup entry layer, and that is the correct scope.

---

## What This Module Owns

This module owns:

- JVM entry
- agent arg parsing
- mod lib path resolution
- jar discovery
- class loader creation
- bootstrap invocation
- startup logging

It does **not** own:

- bridge method registration
- runtime dispatch behavior
- engine Lua exposure
- identity lifecycle services
- ownership logic
- permission logic
- role logic
- debug UI behavior

Those belong to Loader, Bridge, Engine, and the in-game mod package.

---

## Why This Module Is Strong In Its Current Shape

One of the better architectural choices in the repo is keeping the agent narrow.

The agent is best when it does four things well:

- enter early,
- load jars,
- invoke bootstrap,
- fail loudly when boot is broken.

That is what the current module already does.

It is not overloaded with service logic, UI knowledge, or game-facing policy. It is a clean handoff layer, which is exactly what you want from a framework boot module.

---

## Current Reality Of The Module

The right way to document this module is not “this module may someday start the runtime.”

The right way is:

> `PirchsPZAgent` already starts the runtime now.

It is the framework’s current Java entry point.
