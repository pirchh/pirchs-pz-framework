# Pirchs PZ DBI

<div align="center">

# Pirchs PZ DBI
### Java-powered backend runtime, bridge layer, and persistence framework for Project Zomboid

> A modular Java runtime injected into the Project Zomboid JVM via a Java agent, providing a structured bridge between Lua and backend services, including identity, authentication, ownership, roles, permissions, and persistent data systems powered by PostgreSQL.

![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Runtime](https://img.shields.io/badge/Runtime-Java%20Agent-blue?style=for-the-badge)
![Bridge](https://img.shields.io/badge/Bridge-Lua%20↔%20Java-purple?style=for-the-badge)
![Database](https://img.shields.io/badge/Database-PostgreSQL-336791?style=for-the-badge&logo=postgresql&logoColor=white)
![Status](https://img.shields.io/badge/Status-Active%20Development-orange?style=for-the-badge)

</div>

---

## Table of Contents

- [Overview](#overview)
- [What This Repository Contains](#what-this-repository-contains)
- [Core Goals](#core-goals)
- [High-Level Architecture](#high-level-architecture)
- [Execution Flow](#execution-flow)
- [Repository Layout](#repository-layout)
- [Module Breakdown](#module-breakdown)
  - [PirchsPZAgent](#pirchspzagent)
  - [PirchsPZLoader](#pirchspzloader)
  - [PirchsPZBridge](#pirchspzbridge)
  - [PirchsPZDBI Mod Package](#pirchspzdbi-mod-package)
- [Bridge Runtime Model](#bridge-runtime-model)
- [Current Functional Surface](#current-functional-surface)
- [Identity Lifecycle](#identity-lifecycle)
- [Auth / Roles / Ownership Model](#auth--roles--ownership-model)
- [Database Layer](#database-layer)
- [Build and Packaging](#build-and-packaging)
- [Configuration](#configuration)
- [Project Zomboid Mod Integration](#project-zomboid-mod-integration)
- [Debug and Manual Testing](#debug-and-manual-testing)
- [Development Workflow](#development-workflow)
- [Troubleshooting Notes](#troubleshooting-notes)
- [Current Status](#current-status)
- [Roadmap Direction](#roadmap-direction)
- [Naming / File Conventions](#naming--file-conventions)
- [License](#license)

---

## Overview

**Pirchs PZ DBI** is a multi-module Project Zomboid backend/runtime project that injects Java-side functionality into the game process, initializes a shared bridge runtime, registers callable bridge methods, manages persistence bootstrapping, and exposes gameplay-adjacent services to Lua through a structured interface.

At a high level, this repository is not just “a mod.” It is a layered system made up of:

- a **Java agent** that gets code into the JVM early
- a **loader runtime** that owns method registration and dispatch
- a **bridge module** that wires real services into that runtime
- a **Project Zomboid mod package** that provides Lua-side lifecycle glue and debug helpers

The current codebase is centered around:

- player identity resolution
- account creation / lookup
- ownership and node assignment
- permission checks
- role assignment
- debug/self-test flows
- schema bootstrapping for a PostgreSQL-backed persistence layer

---

## What This Repository Contains

This repository currently combines four distinct layers:

1. **PirchsPZAgent**  
   The JVM entry bootstrap used with `-javaagent`.

2. **PirchsPZLoader**  
   A lightweight shared runtime kernel that provides bridge method registration, request modeling, dispatch, and result wrapping.

3. **PirchsPZBridge**  
   The actual application/service layer that initializes schemas, registers real bridge endpoints, and wires identity/auth/persistence behavior into the loader runtime.

4. **PirchsPZDBI/42**  
   The Project Zomboid mod package containing Lua files and mod metadata used to connect the in-game lifecycle to the Java side.

---

## Core Goals

This project is built around a few practical goals:

- move important backend-style logic out of fragile ad hoc Lua flows
- keep persistence and identity handling on the Java side
- expose a clean callable bridge surface instead of spreading raw DB logic into gameplay scripts
- establish a durable foundation for systems like:
  - banking
  - account-bound progression
  - ownership
  - permissions
  - roles
  - future admin / economy / world-state systems

---

## High-Level Architecture

```text
Project Zomboid JVM
    |
    v
-javaagent: PirchsPZAgent
    |
    v
PirchsPZLoader
    |
    v
PirchsPZBridge
    |
    +--> Database bootstrapping
    +--> Schema initialization
    +--> Identity lifecycle wiring
    +--> Bridge endpoint registration
    |
    v
Lua-side mod package (PirchsPZDBI/42)
    |
    +--> player-ready signaling
    +--> local identity readiness flow
    +--> debug smoke tests
```

---

## Execution Flow

### 1) JVM bootstrap
Project Zomboid launches with the Java agent jar attached.

### 2) Agent entry
The agent initializes early and is responsible for loading the bridge/runtime jars into the game JVM.

### 3) Loader startup
The shared loader runtime initializes once and becomes the common method registration/dispatch layer.

### 4) Bridge startup
The bridge initializes schemas, seeds auth bootstrap data, registers system/bank/ownership/permission/role bridge methods, and optionally registers debug methods.

### 5) Identity lifecycle arming
The Java side arms identity lifecycle support and the fallback discovery watcher.

### 6) Lua notifies readiness
Lua-side scripts wait for the proper player lifecycle moment, then signal that the local player is ready for identity resolution.

### 7) Runtime bridge usage
Lua or other callers invoke named methods through the bridge runtime rather than directly coupling to every Java service implementation.

---

## Repository Layout

```text
pirchs-pz-dbi/
├─ PirchsPZAgent/
│  ├─ build.gradle
│  └─ src/main/
│     ├─ java/pirch/pzagent/
│     │  ├─ AgentEntry.java
│     │  ├─ AgentLog.java
│     │  └─ ModJarLoader.java
│     └─ resources/META-INF/
│
├─ PirchsPZLoader/
│  ├─ build.gradle
│  ├─ settings.gradle
│  └─ src/main/java/pirch/pzloader/
│     ├─ bootstrap/
│     │  └─ LoaderBootstrap.java
│     ├─ runtime/
│     │  ├─ BridgeMethodDefinition.java
│     │  ├─ BridgeRequest.java
│     │  ├─ BridgeResult.java
│     │  ├─ InvocationDispatcher.java
│     │  └─ ModuleRegistry.java
│     └─ util/
│        └─ LoaderLog.java
│
├─ PirchsPZBridge/
│  ├─ build.gradle
│  ├─ settings.gradle
│  └─ src/main/java/pirch/pz/
│     ├─ BridgeBootstrap.java
│     ├─ bridge/
│     │  ├─ BankBridge.java
│     │  ├─ DebugBridge.java
│     │  ├─ OwnershipBridge.java
│     │  ├─ PermissionBridge.java
│     │  ├─ RoleBridge.java
│     │  └─ SystemBridge.java
│     ├─ db/
│     │  ├─ DatabaseConfig.java
│     │  ├─ DatabaseManager.java
│     │  └─ SchemaManager.java
│     ├─ debug/
│     │  ├─ IdentityDiscoveryWatcher.java
│     │  ├─ IdentityLifecycleBridge.java
│     │  └─ IdentityTestHook.java
│     ├─ repo/
│     │  └─ PostgresAccountRepository.java
│     └─ service/
│        ├─ AccountService.java
│        ├─ BankService.java
│        ├─ PlayerContextResolver.java
│        ├─ PlayerIdentity.java
│        ├─ PlayerIdentityService.java
│        ├─ PzPlayerIdentityAdapter.java
│        ├─ AuthSelfTestService.java
│        ├─ BootstrapAdminService.java
│        ├─ PzRuntimeConfig.java
│        └─ RoleService.java
│
├─ PirchsPZDBI/
│  └─ 42/
│     ├─ lib/
│     ├─ media/lua/
│     │  ├─ client/
│     │  │  ├─ PirchsPZDBI_Debug.lua
│     │  │  └─ PirchsPZDBI_IdentityReady.lua
│     │  └─ server/
│     │     └─ PirchsPZDBI_ServerIdentityReady.lua
│     └─ mod.info
│
├─ docs/
├─ build.gradle
├─ settings.gradle
├─ gradle.properties.example
├─ local.dev.properties.example
├─ gradlew
└─ gradlew.bat
```

---

## Module Breakdown

## PirchsPZAgent

### Purpose
`PirchsPZAgent` is the early-entry bootstrap layer for the runtime stack.

### What it does
- defines the Java premain entry
- carries agent manifest metadata
- loads supporting jars/modules into the Project Zomboid JVM
- serves as the earliest reliable hook point before the gameplay-facing bridge layer takes over

### Important files
- `AgentEntry.java`
- `AgentLog.java`
- `ModJarLoader.java`

### Why it matters
Without the agent layer, the rest of the Java-side runtime cannot reliably establish itself at JVM startup. This is the “get into the process cleanly” piece of the architecture.

---

## PirchsPZLoader

### Purpose
`PirchsPZLoader` is the minimal shared runtime kernel.

### What it owns
- one-time loader bootstrap
- method registration
- request shaping
- invocation dispatch
- success/failure result wrapping
- centralized runtime logging

### Key runtime classes

#### `LoaderBootstrap`
Handles one-time loader initialization and protects against double-init.

#### `ModuleRegistry`
Stores registered bridge methods and their definitions.  
This is the registry that bridge modules populate.

#### `InvocationDispatcher`
Routes a named request to a registered handler and wraps the response as a `BridgeResult`.

#### `BridgeRequest`
Normalizes the incoming method name and argument array.

#### `BridgeResult`
Normal success/failure wrapper for method calls.

#### `BridgeMethodDefinition`
Metadata holder for registered methods, such as name/version/min-args.

### Why it matters
This module keeps the actual application/service layer from devolving into a pile of static calls. It gives the project a stable, reusable runtime contract for bridge method exposure.

---

## PirchsPZBridge

### Purpose
`PirchsPZBridge` is the real application layer of the repository.

### What it does during initialization
- initializes the shared loader
- initializes database schemas
- seeds auth bootstrap data
- registers bridge method groups
- enables optional debug bridge behavior
- arms identity lifecycle readiness and fallback detection

### Registered bridge groups
- `SystemBridge`
- `BankBridge`
- `OwnershipBridge`
- `PermissionBridge`
- `RoleBridge`
- `DebugBridge` (when enabled)

### What lives here
- bridge endpoint registrations
- database config and schema init
- repository/data access classes
- identity resolution logic
- role / permission / ownership services
- runtime config helpers
- auth self-test behavior

### Why it matters
This module is where the project stops being “just infrastructure” and starts becoming a game/runtime backend.

---

## PirchsPZDBI Mod Package

### Purpose
This is the Project Zomboid mod-facing package that lives under `PirchsPZDBI/42`.

### What it currently provides
- mod metadata via `mod.info`
- Lua-side local player readiness signaling
- debug-side smoke testing hooks
- the in-game connection layer back to the Java runtime

### Important Lua files

#### `PirchsPZDBI_IdentityReady.lua`
Watches for player availability and then either:
- calls Java directly in single-player / local contexts, or
- sends a client command in client/server contexts

#### `PirchsPZDBI_Debug.lua`
Arms a one-shot smoke test path that waits for:
- player object readiness
- bridge readiness
- local identity readiness

Then triggers:
- `localSnapshot()`
- `selfTestNow()`
- `runSmokeSuite(...)`

#### `PirchsPZDBI_ServerIdentityReady.lua`
Server-side lifecycle support file included in the mod package.

---

## Bridge Runtime Model

The loader/bridge arrangement is intentionally simple:

```text
caller
  -> method name + args
  -> BridgeRequest
  -> InvocationDispatcher
  -> ModuleRegistry lookup
  -> registered handler
  -> BridgeResult
```

This gives you a few important benefits:

- easier debugging
- consistent error handling
- method-level versioning/metadata
- safer expansion over time
- less direct coupling between Lua callers and raw implementation classes

---

## Current Functional Surface

Based on the current repository structure and initialization path, the bridge surface is focused on:

- system/runtime methods
- bank-related methods
- ownership methods
- permission methods
- role methods
- optional debug helpers

The current top-level README in the repo explicitly documents the temporary debug testing surface below:

- `pz.bridge.debug.selfTestNow`
- `pz.bridge.debug.selfTestStatus`
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

That debug surface exists specifically so auth/ownership/role behavior can be exercised locally without manually editing the database every single time.

---

## Identity Lifecycle

Identity is one of the central concerns of the project.

### Current direction
The current codebase is built around a **single lifecycle promotion path** with a **watcher fallback**.

### Java-side pieces
- `IdentityLifecycleBridge`
- `IdentityDiscoveryWatcher`
- `IdentityTestHook`
- `PlayerIdentity`
- `PlayerIdentityService`
- `PzPlayerIdentityAdapter`
- `PlayerContextResolver`

### Lua-side pieces
- `PirchsPZDBI_IdentityReady.lua`
- `PirchsPZDBI_ServerIdentityReady.lua`

### Practical intent
The goal is to avoid trying to do all identity authority in ad hoc Lua while still using Lua to tell the Java side when the player is actually available and meaningful.

### Why this matters
A lot of higher-level systems depend on identity being solved first:

- account resolution
- role membership
- permission checks
- ownership attribution
- self-test execution
- future economy/account-bound state

---

## Auth / Roles / Ownership Model

The current bridge bootstrap logs and registrations make the direction here pretty clear.

### Ownership
The project is currently built around **single-owner nodes** with delegated scoped access.

### Permissions
Permissions are treated as structured access checks, not just arbitrary flags floating around in Lua.

### Roles
Roles exist as bundles of permissions for:
- global access
- node-scoped access

### Bootstrap behavior
At startup the bridge seeds/ensures auth bootstrap state, including core roles and optional bootstrap admin setup.

### Why this matters
This gives the repository a real access-control foundation that can later support:
- admin tooling
- property ownership
- banking privileges
- role-based gameplay systems
- scoped interactions with world entities and persistent nodes

---

## Database Layer

### Current backend
The current code is configured for **PostgreSQL**.

That is important: the current codebase is not describing a local SQLite-first persistence layer. The repository build/config currently points to PostgreSQL.

### Important classes
- `DatabaseConfig`
- `DatabaseManager`
- `SchemaManager`
- `PostgresAccountRepository`

### Default database assumptions
The current config code expects:
- host: `127.0.0.1`
- port: `5432`
- database: `pzlife`
- user: `postgres`
- password: `postgres`
- SSL: optional / default false

### Schema bootstrapping
`SchemaManager` currently knows how to initialize a set of SQL resources including:

- `001_schema.sql`
- `002_accounts.sql`
- `002_wallet.sql`
- `002_nodes.sql`
- `002_account_permissions.sql`
- `003_transactions.sql`
- `003_account_nodes.sql`
- `003_account_node.sql`
- `004_account_identity_columns.sql`
- `004_roles.sql`
- `005_role_permission.sql`
- `006_account_role.sql`

### Schema folder roots
The database config points to schema locations under:

- `sql/accounts`
- `sql/economy`
- `sql/ownership`
- `sql/permissions`

### Why it matters
The repository is clearly moving toward a normalized persistent model that supports:
- accounts
- wallets / transactions
- nodes / ownership
- permissions
- roles

---

## Build and Packaging

### Root build
The root Gradle build applies Java tooling to subprojects and sets:

- group: `pirch`
- version: `0.0.3`

### Java toolchain
The build resolves the Java version from:

1. Gradle property `pzJavaVersion`
2. environment variable `PZ_JAVA_VERSION`
3. fallback default: `25`

### Included Gradle modules
`settings.gradle` currently includes:

- `PirchsPZLoader`
- `PirchsPZBridge`
- `PirchsPZAgent`

### Agent jar
The agent jar manifest declares:

- `Premain-Class: pirch.pzagent.AgentEntry`
- redefine/retransform flags disabled

### Bridge dependencies
The bridge currently depends on:
- `PirchsPZLoader`
- PostgreSQL JDBC driver `42.7.3`

### PZ compile classpath support
The bridge build script can compile against Project Zomboid jars when a game/lib directory is configured.

---

## Configuration

There are two major config categories in the current code:

### 1) Build-time / local-dev configuration
Used to help Gradle find local Project Zomboid jars and local environment values.

Relevant files:
- `gradle.properties.example`
- `local.dev.properties.example`

Relevant properties/environment variables include:
- `pzJavaVersion`
- `PZ_JAVA_VERSION`
- `pzLibDir`
- `PZ_LIB_DIR`
- `pzGameDir`
- `PZ_GAME_DIR`

### 2) Runtime configuration
Loaded from `pirchdb.properties`.

This currently controls things like:
- database enablement
- database URL / host / port / name / credentials
- schema auto-init
- schema locations
- identity detector enablement
- identity detector poll timing
- max attempts
- log frequency
- monitor-after-resolve behavior
- verbose identity logging

### Known auth/debug flags in active use
The current repo README and bridge bootstrap reference flags such as:

- `debug.bridge.enabled`
- `auth.selftest.enabled`

Depending on your local setup, you may also be using additional bootstrap or identity-related flags.

---

## Project Zomboid Mod Integration

### Mod metadata
Current `mod.info` declares:

- name: `PirchsPZDBI`
- id: `pirchs_pz_dbi`

### Role of the mod package
The mod package is the “in-game glue” that connects:
- player lifecycle timing
- Lua event callbacks
- Java bridge readiness
- manual debug flows

### Why it is separate
The Java runtime and the Zomboid mod package serve different jobs:

- the Java side owns persistence/runtime behavior
- the Lua side owns in-engine timing, event listening, and practical handoff moments

That separation is useful because it keeps gameplay/event timing concerns out of the core Java runtime as much as possible.

---

## Debug and Manual Testing

The current codebase contains an explicit debug/manual testing path.

### Current debug flow
The repo README describes this intended sequence:

1. start the game with the Java agent
2. let the world finish loading
3. wait for the lifecycle log line showing the local account resolved
4. then invoke debug methods

### Why the timing matters
The debug methods assume the local player/account has already been resolved by the identity lifecycle watcher or lifecycle bridge path.

### Current one-shot Lua smoke script
`PirchsPZDBI_Debug.lua` waits until:
- a player object exists
- `pz.bridge.debug` exists
- `isLocalIdentityReady()` says true

Then it runs:
- local snapshot
- self-test
- smoke suite

### Practical use
This is extremely useful while stabilizing:
- bootstrap flow
- bridge exposure
- auth state
- ownership logic
- permissions/roles
- “is the Java side actually ready yet?” problems

---

## Development Workflow

### Typical local flow

#### 1) Work in a feature branch
```bash
git checkout -b feature/your-change
```

#### 2) Update Java and/or Lua pieces
Depending on what you are working on:
- agent bootstrap
- runtime dispatch
- bridge registration
- database/service logic
- Lua lifecycle integration
- docs/debug helpers

#### 3) Build jars
Use Gradle from the repository root.

```bash
gradlew build
```

On Windows:
```bash
gradlew.bat build
```

#### 4) Package/deploy to your mod/lib setup
Copy the produced jars into the appropriate Project Zomboid mod/lib layout used by your local environment.

#### 5) Launch PZ with the agent enabled
Make sure the Java agent and dependent jars are actually present.

#### 6) Test in game
Check:
- Java-side initialization logs
- identity readiness logs
- bridge availability
- Lua smoke/debug behavior

### Helpful branch naming examples
- `feature/auth-bootstrap`
- `feature/debug-bridge-ui`
- `feature/identity-lifecycle-fix`
- `fix/lua-readiness-order`
- `update/readme-refresh`

---

## Troubleshooting Notes

### “Bridge not ready yet”
Usually means one of these:
- the agent did not load correctly
- jars are missing from the expected location
- bridge bootstrap did not complete
- Lua fired before Java-side readiness

### “Local identity not ready yet”
Usually means:
- the local player lifecycle path has not completed yet
- the watcher has not resolved identity yet
- Lua is trying to call debug methods too early

### Duplicate initialization issues
The loader and bridge both contain explicit “already initialized” protections.  
If something appears to initialize twice, check your load path and mod/jar duplication before assuming the guards failed.

### Game jars not found during compile
Set one of:
- `-PpzLibDir="...ProjectZomboid"`
- `PZ_LIB_DIR=...`
- `PZ_GAME_DIR=...`

### Database bootstrapping problems
Check:
- PostgreSQL is running
- credentials match `pirchdb.properties`
- schema auto-init is enabled if you expect automatic creation
- SQL resources exist in the expected packaged locations

### Debug bridge methods missing
Check:
- `debug.bridge.enabled=true`
- bridge bootstrap completed
- local identity has resolved before you try to use identity-dependent methods

---

## Current Status

As of the current code structure, this repository already has a solid foundation in place for:

- agent-based JVM entry
- runtime loader bootstrapping
- named bridge method registration/dispatch
- PostgreSQL-backed persistence configuration
- schema auto-initialization
- account/identity plumbing
- ownership/permission/role surfaces
- Lua readiness hooks
- debug/self-test helpers

What is still evolving is the refinement and stabilization of:
- exact identity timing
- Lua/UI debug ergonomics
- broader gameplay-facing bridge surface
- full operational documentation for setup/deployment/testing

---

## Roadmap Direction

Without overpromising beyond the current code, the structure strongly points toward continued work in these areas:

- richer bridge APIs
- stronger identity/session handling
- more complete bank/economy flows
- admin/bootstrap tooling
- more polished debug UI and testing flows
- cleaner packaging/deployment workflow
- expanded permission and role semantics
- future persistent gameplay systems built on top of the same account/ownership model

---

## Naming / File Conventions

### README capitalization
For GitHub repositories, use:

```text
README.md
```

That is the standard and preferred capitalization.

### Project naming
Current names in repo are intentionally layered:

- **PirchsPZAgent** = JVM entry
- **PirchsPZLoader** = shared runtime kernel
- **PirchsPZBridge** = service/bridge layer
- **PirchsPZDBI** = mod package / user-facing mod identity

That naming makes sense for the current architecture because each module has a distinct role.

---

## License

No license file is clearly documented in the currently inspected repository root, so do **not** assume one in public-facing docs unless you intentionally add it.

If you want this project to be clearly open, source-available, or private-with-permissions, add an explicit license file and update this section accordingly.

---

## Maintainer Notes

This repository makes the most sense when you think of it as a **runtime platform** for Project Zomboid systems, not just a one-off bridge hack.

The architecture is already split the right way:

- bootstrap early
- keep the core runtime generic
- wire game-facing services on top
- use Lua for timing/event glue
- keep persistence and structured logic on the Java side

That separation is what gives this project room to grow into something much larger than a single feature mod.
