# PirchsPZLoader README

## Module Role

`PirchsPZLoader` is the runtime contract module for the repo.

This module is the shared backbone behind bridge registration and invocation. It gives the project explicit method metadata, explicit requests, explicit results, and a registry/dispatcher pair that turns named bridge calls into structured Java execution.

This is not theoretical architecture. These classes are already the actual bridge runtime used by the repo.

---

## Functional Shape

### `pirch.pzloader.bootstrap.LoaderBootstrap`
This class owns one-time loader initialization.

It currently defines:
- `initialize()`
- `isInitialized()`

#### `initialize()`
This method:
1. checks whether the loader is already initialized,
2. logs either “already initialized” or “initializing loader,”
3. flips the initialized flag,
4. logs completion.

#### `isInitialized()`
Returns the current initialization state.

This makes loader boot idempotent, which is exactly what a shared runtime kernel should do.

---

### `pirch.pzloader.runtime.BridgeMethodDefinition`
This is the method metadata object for registered bridge methods.

It currently stores:
- `methodName`
- `version`
- `description`
- `minArgCount`

It provides:
- `builder(String methodName)`
- `getMethodName()`
- `getVersion()`
- `getDescription()`
- `getMinArgCount()`

The nested `Builder` supports:
- `version(String version)`
- `description(String description)`
- `minArgCount(int minArgCount)`
- `build()`

This means every registered method can carry both functional behavior and runtime metadata.

---

### `pirch.pzloader.runtime.BridgeRequest`
This is the normalized method invocation input.

It currently defines:
- `of(String methodName, Object... args)`
- `getMethodName()`
- `getArgs()`
- `argCount()`

The class copies its argument array defensively. That keeps request objects stable after construction.

---

### `pirch.pzloader.runtime.BridgeResult`
This is the normalized return object for bridge calls.

It currently defines:
- `ok(Object data)`
- `fail(String error)`
- `isSuccess()`
- `getData()`
- `getError()`
- `toString()`

This gives every bridge call a predictable success/failure shape.

---

### `pirch.pzloader.runtime.ModuleRegistry`
This is the global runtime registry for bridge methods.

It currently defines:
- `register(String methodName, BridgeMethod handler)`
- `register(BridgeMethodDefinition definition, BridgeMethod handler)`
- `get(String methodName)`
- `getDefinition(String methodName)`
- `has(String methodName)`
- `count()`
- `getAllDefinitions()`

The registry stores methods in insertion order using `LinkedHashMap`.

Important runtime behavior:
- legacy registration is supported through `register(String, BridgeMethod)`
- duplicate registration overwrites the previous entry and logs that overwrite
- metadata and handler are stored together as a `RegisteredMethod`

This class is the single source of truth for what the bridge exposes.

---

### `pirch.pzloader.runtime.InvocationDispatcher`
This is the execution path for registered methods.

It currently defines:
- `invoke(String methodName, Object... args)`
- `invoke(BridgeRequest request)`

The dispatcher does real runtime validation:
1. rejects null requests,
2. rejects blank method names,
3. checks the registry for the handler,
4. validates minimum argument count against `BridgeMethodDefinition`,
5. invokes the registered handler,
6. returns a `BridgeResult`,
7. wraps plain objects as `BridgeResult.ok(...)`,
8. catches exceptions and returns `BridgeResult.fail(...)`.

That is the core runtime contract of the repo.

---

## What This Module Owns

This module owns:
- runtime initialization
- method metadata
- request wrapping
- result wrapping
- method registration
- method lookup
- invocation dispatch
- loader-side logging

It does **not** own:
- the actual debug methods
- the Lua globals
- the engine patch
- the UI
- the identity event hooks

Those live in the bridge, engine, and DBI modules.

---

## Current Value To The Repo

The loader gives the repo a real callable runtime instead of a pile of direct static calls.

That matters because the rest of the stack can now rely on:
- named methods
- discoverable metadata
- argument validation
- consistent return shapes
- centralized dispatch

That is exactly the kind of boring infrastructure a bridge system needs.
