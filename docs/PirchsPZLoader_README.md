# PirchsPZLoader README

## Module Role

`PirchsPZLoader` is the runtime contract module for the framework.

This module is the shared backbone behind method registration and named bridge invocation. It gives the framework:

- explicit method metadata,
- explicit requests,
- explicit results,
- a registry,
- a dispatcher,
- and loader initialization state.

This is not theoretical architecture. These classes are already the actual callable runtime used by the rest of the repo.

---

## Why This Module Matters

Without this module, the framework would degrade into ad hoc static calls and manually coordinated behavior between Java and Lua.

With this module, the framework gets:

- named methods
- discoverable metadata
- argument validation
- consistent success/failure results
- centralized registration
- centralized invocation

That is exactly the kind of boring infrastructure a bridge runtime needs.

---

## Functional Shape

### `pirch.pzloader.bootstrap.LoaderBootstrap`

This class owns one-time loader initialization.

It currently defines:

- `initialize()`
- `isInitialized()`

The loader bootstrap is intentionally simple, because its job is to establish loader init state and stay out of the way.

---

## `initialize()`

`initialize()`:

1. checks whether the loader is already initialized,
2. logs either “already initialized” or “initializing loader,”
3. flips the initialized flag,
4. logs completion.

That makes loader boot idempotent, which is exactly what a shared runtime kernel should be.

---

## `isInitialized()`

Returns the current initialization state.

That sounds trivial, but it matters because bridge/runtime code can now query a stable loader state instead of guessing.

---

## `BridgeMethodDefinition`

### `pirch.pzloader.runtime.BridgeMethodDefinition`

This is the metadata object for registered bridge methods.

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

The nested builder supports:

- `version(String version)`
- `description(String description)`
- `minArgCount(int minArgCount)`
- `build()`

This allows the framework to treat registered methods as discoverable runtime objects, not anonymous lambdas with no metadata.

---

## Why Method Metadata Matters

The bridge is already benefiting from explicit method metadata because it allows the framework to support:

- diagnostics
- runtime introspection
- method listing
- min-arg validation
- clearer docs
- bridge snapshots

That becomes even more important as the framework grows beyond the current debug surface.

---

## `BridgeRequest`

### `pirch.pzloader.runtime.BridgeRequest`

This class is the normalized method invocation input.

It currently defines:

- `of(String methodName, Object... args)`
- `getMethodName()`
- `getArgs()`
- `argCount()`

The class defensively copies its argument array, which keeps the request object stable after construction.

That is the correct design for a runtime request wrapper.

---

## `BridgeResult`

### `pirch.pzloader.runtime.BridgeResult`

This class is the normalized return wrapper for bridge calls.

It currently defines:

- `ok(Object data)`
- `fail(String error)`
- `isSuccess()`
- `getData()`
- `getError()`
- `toString()`

That gives every bridge call a predictable result shape.

This matters a lot because the Lua side should not have to guess whether a Java call:
- threw,
- returned null,
- returned an object,
- or failed logically.

The framework can standardize all of that through `BridgeResult`.

---

## `ModuleRegistry`

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

That makes it the single source of truth for what the bridge exposes.

---

## Registry Behavior

Important current runtime behavior:

- legacy registration is still supported through `register(String, BridgeMethod)`
- duplicate registration overwrites the prior entry and logs the overwrite
- metadata and handler are stored together as a registered method unit
- the registry can be introspected for diagnostics

This is exactly the kind of practical runtime behavior you want during framework growth.

---

## `InvocationDispatcher`

### `pirch.pzloader.runtime.InvocationDispatcher`

This is the actual execution path for registered methods.

It currently defines:

- `invoke(String methodName, Object... args)`
- `invoke(BridgeRequest request)`

The dispatcher performs real validation and normalization:

1. rejects null requests,
2. rejects blank method names,
3. checks the registry for the handler,
4. validates minimum argument count against `BridgeMethodDefinition`,
5. invokes the registered handler,
6. returns a `BridgeResult`,
7. wraps plain objects as `BridgeResult.ok(...)`,
8. catches exceptions and returns `BridgeResult.fail(...)`.

That is the core runtime contract of the framework.

---

## Why Dispatch Centralization Matters

Central dispatch is one of the strongest parts of the current design.

It means the framework can evolve without every caller needing to know:
- which class owns the work,
- how errors are returned,
- how arg counts are validated,
- or how method availability is checked.

Everything goes through the runtime contract.

That is what gives the bridge layer its shape.

---

## What This Module Owns

This module owns:

- runtime initialization
- method metadata
- request wrapping
- result wrapping
- registration
- lookup
- dispatch
- loader-side logging/state

It does **not** own:

- the actual identity/ownership/permission/role methods
- the engine patch
- the Lua globals
- the in-game UI
- the client/server readiness signals

Those belong to Bridge, Engine, and the mod package.

---

## Current Value To The Framework

The loader gives the framework a real callable runtime instead of a pile of direct static calls.

That matters because the rest of the stack can rely on:

- named methods
- discoverable metadata
- argument validation
- consistent result shapes
- centralized dispatch
- runtime snapshots and diagnostics

It is exactly the kind of foundational infrastructure a framework project needs.

---

## Current Reality Of The Module

The right way to document this module is not “this module may later support a bridge.”

The right way is:

> `PirchsPZLoader` is already the runtime contract that the bridge uses today.

It is the framework kernel behind named method execution.
