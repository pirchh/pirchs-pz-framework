# PirchsPZAgent README

## Module Role

`PirchsPZAgent` is the JVM boot module for the repo.

This module starts the entire stack. Its job is not vague and it is not “intended” to do startup later. It already does startup now.

The module takes control at JVM premain time, resolves the mod jar location, loads the jars in the mod lib folder, and invokes the configured bootstrap class.

---

## Functional Shape

### `pirch.pzagent.AgentEntry`
`AgentEntry` is the agent entry class and implements the runtime boot sequence.

It currently defines:

- `premain(String agentArgs, Instrumentation inst)`
- `premain(String agentArgs)`
- `start(String agentArgs)`
- `resolveModLibPath(Map<String, String> parsedArgs)`
- `parseAgentArgs(String agentArgs)`

What those methods do:

#### `premain(...)`
Both `premain` overloads route directly into `start(...)`.

#### `start(...)`
`start(...)` performs the actual boot work:
1. logs startup,
2. parses agent args,
3. resolves the mod lib path,
4. resolves the bootstrap class name,
5. loads the mod jars with `ModJarLoader.loadModLibFolder(...)`,
6. loads the bootstrap class with the mod class loader,
7. reflects the `initialize()` method,
8. invokes it.

The current default values are:
- default mod lib: `C:/Users/ryanj/Zomboid/mods/PirchsPZDBI/42/lib`
- default bootstrap class: `pirch.pz.BridgeBootstrap`

#### `resolveModLibPath(...)`
This method resolves the mod lib path in a concrete fallback order:
1. `modLib` from agent args
2. `pirch.pz.modLib` system property
3. `PIRCHS_PZ_MOD_LIB` environment variable
4. built-in default path

That means the agent already supports multiple launch styles without code changes.

#### `parseAgentArgs(...)`
This method parses semicolon-delimited `key=value` entries such as:

```text
modLib=C:/Users/ryanj/Zomboid/mods/PirchsPZDBI/42/lib;bootstrapClass=pirch.pz.BridgeBootstrap
```

It stores them in insertion order using `LinkedHashMap`.

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

If the folder is missing or contains no jars, the method fails immediately. That is the correct behavior for this module because silent partial startup would make debugging much worse.

---

## Logging

### `pirch.pzagent.AgentLog`
The agent module also includes a dedicated logging helper. That matters because agent startup is one of the most failure-prone phases in the repo.

The agent log is what tells you:
- startup began,
- which mod lib path was chosen,
- which bootstrap class was chosen,
- which jars were found,
- and whether bootstrap invocation succeeded or failed.

---

## Build Shape

The module jar is explicitly configured with:
- `archiveBaseName = 'PirchsPZAgent'`
- `Premain-Class = pirch.pzagent.AgentEntry`
- `Can-Redefine-Classes = false`
- `Can-Retransform-Classes = false`

That tells you exactly what this module is:
a Java agent bootstrap jar, not a bytecode rewriting tool.

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
- Lua exposure
- identity logic
- permissions
- roles
- ownership behavior
- debug UI behavior

Those are downstream layers.

---

## Why This Split Is Correct

Keeping the agent narrow is one of the strongest choices in the repo.

The agent is best when it does four things well:
- enter early,
- load the jars,
- invoke bootstrap,
- fail loudly if startup is broken.

That is exactly what the current code does.
