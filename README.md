# Pirchs PZ DBI

<div align="center">

# Pirchs PZ DBI
### Java runtime, bridge, identity, ownership, permissions, and PostgreSQL-backed persistence for Project Zomboid

> A multi-module Java runtime for injecting backend services into the Project Zomboid JVM, registering bridge methods, resolving stable account and character identity, and persisting account, economy, ownership, and authorization data in PostgreSQL.

![Java](https://img.shields.io/badge/Java-25-blue?style=for-the-badge)
![Gradle](https://img.shields.io/badge/Gradle-Multi--Project-02303A?style=for-the-badge&logo=gradle)
![Project Zomboid](https://img.shields.io/badge/Project%20Zomboid-Modding-6D8E23?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Active%20Prototype-orange?style=for-the-badge)
![Database](https://img.shields.io/badge/Database-PostgreSQL-336791?style=for-the-badge&logo=postgresql)

</div>

---

## Table of Contents

- [What This Repo Is](#what-this-repo-is)
- [Current State](#current-state)
- [What Changed Recently](#what-changed-recently)
- [Architecture](#architecture)
- [Repo Layout](#repo-layout)
- [Modules](#modules)
- [Bridge API](#bridge-api)
- [Identity Model](#identity-model)
- [Ownership and Permissions Model](#ownership-and-permissions-model)
- [Database Layer](#database-layer)
- [Build and Install](#build-and-install)
- [Runtime Boot Sequence](#runtime-boot-sequence)
- [Configuration](#configuration)
- [Development Notes](#development-notes)
- [Testing](#testing)
- [Known Gaps / Next Pass](#known-gaps--next-pass)
- [Maintainer Notes](#maintainer-notes)

---

## What This Repo Is

**Pirchs PZ DBI** is a **Gradle multi-project Java runtime** for Project Zomboid.

Despite the name, this repository is not just a thin DB helper. It currently contains three coordinated modules that together form the Java-side runtime foundation for persistent gameplay systems:

1. **PirchsPZAgent**  
   A Java agent that starts before the rest of the runtime, resolves the mod `lib` folder, loads jars, and invokes the bridge bootstrap.

2. **PirchsPZLoader**  
   The runtime core responsible for bridge registration, method metadata, invocation dispatch, validation, and structured bridge results.

3. **PirchsPZBridge**  
   The bridge, services, repositories, schema bootstrap, identity lifecycle logic, banking, ownership, permissions, and PostgreSQL integration.

The current implementation is explicitly **PostgreSQL-backed**.

It is not pretending to be database-agnostic right now.

---

## Current State

This repo now represents a real Java-side backend foundation for Project Zomboid rather than just an early identity experiment.

At a high level, the runtime now supports:

- Java agent startup into the Project Zomboid JVM
- runtime method registration and dispatch
- structured player identity normalization
- account resolution backed by PostgreSQL
- wallet creation and balance-changing transactions
- ownership claims for world nodes
- scoped permissions and authorization checks
- lifecycle-aware local player resolution
- Java-side auth self-test flow after account resolution
- bootstrap admin and role foundation for the next authorization pass

The important practical shift is this:

```text
Project Zomboid player context
        ->
structured identity
        ->
stable account external id
        ->
account + character persistence
        ->
ownership / permissions / economy systems
```

That is the real center of the repo now.

---

## What Changed Recently

This README reflects the current codebase after the recent identity, lifecycle, ownership, permissions, and self-test passes.

### Major additions now present

- session-aware Java-side identity lifecycle handling
- Java-side discovery watcher that stays armed across session changes
- explicit lifecycle snapshot/state reporting
- structured account and character external IDs
- ownership bridge and service layer
- permissions bridge and service layer
- owner-implied access rules for scoped node permissions
- Java-side auth self-test that runs after local account resolution
- runtime config helpers for detector, logging, auth, and bootstrap-admin behavior
- role/bootstrap-admin foundation for the next auth pass
- example local development property files
- initial JUnit coverage for identity normalization
- bridge version bumped to `0.0.3`

### Practical effect

The repo is no longer just about resolving an external ID and storing money.

It now has the beginning of a real backend platform model for Project Zomboid:

- stable account identity
- separate character identity shape
- economy data
- ownership data
- authorization data
- runtime health + lifecycle introspection

---

## Architecture

```text
Project Zomboid JVM
        |
        v
PirchsPZAgent
  - premain()
  - resolves mod lib path
  - loads jars from mod lib folder
  - invokes pirch.pz.BridgeBootstrap
        |
        v
PirchsPZBridge
  - initializes loader
  - initializes schema
  - registers bridge modules
  - arms identity lifecycle
  - starts Java-side discovery watcher
        |
        v
PirchsPZLoader
  - ModuleRegistry
  - BridgeMethodDefinition
  - BridgeRequest
  - InvocationDispatcher
  - BridgeResult
        |
        v
Services / Repositories
  - PlayerIdentityService
  - IdentityLifecycleService
  - AccountService
  - BankService
  - OwnershipService
  - PermissionService
  - RoleService
  - BootstrapAdminService
  - AuthSelfTestService
  - Postgres*Repository classes
        |
        v
PostgreSQL
  - accounts schema
  - economy schema
  - ownership schema
  - permissions schema
```

### Runtime direction

The loader owns generic runtime plumbing.
The bridge owns gameplay-facing backend behavior.
The services own business logic.
The repositories own persistence.

That separation is important and is already visible in the current codebase.

---

## Repo Layout

```text
pirchs-pz-dbi/
|
|-- build.gradle
|-- settings.gradle
|-- gradle.properties.example
|-- local.dev.properties.example
|-- README.md
|-- docs/
|   |-- AUTH_ARCHITECTURE_NEXT_PASS.md
|   |-- AUTH_SELF_TEST.md
|   |-- BRIDGE_API.md
|   |-- FIX_NOTES_BRIDGE_SERVICE_ALIGNMENT.md
|   |-- IMPLEMENTATION_NOTES.md
|   |-- JAVA_DETECTOR_NOTES.md
|   `-- OWNERSHIP_PERMISSIONS_V1.md
|
|-- PirchsPZAgent/
|   |-- build.gradle
|   `-- src/main/
|       |-- java/pirch/pzagent/
|       |   |-- AgentEntry.java
|       |   |-- AgentLog.java
|       |   `-- ModJarLoader.java
|       `-- resources/META-INF/
|
|-- PirchsPZLoader/
|   |-- build.gradle
|   `-- src/main/java/pirch/pzloader/
|       |-- bootstrap/
|       |   `-- LoaderBootstrap.java
|       |-- runtime/
|       |   |-- BridgeMethodDefinition.java
|       |   |-- BridgeRequest.java
|       |   |-- BridgeResult.java
|       |   |-- InvocationDispatcher.java
|       |   `-- ModuleRegistry.java
|       `-- util/
|           `-- LoaderLog.java
|
`-- PirchsPZBridge/
    |-- build.gradle
    `-- src/
        |-- main/java/pirch/pz/
        |   |-- BridgeBootstrap.java
        |   |-- bridge/
        |   |   |-- BankBridge.java
        |   |   |-- OwnershipBridge.java
        |   |   |-- PermissionBridge.java
        |   |   `-- SystemBridge.java
        |   |-- db/
        |   |   |-- DatabaseConfig.java
        |   |   |-- DatabaseManager.java
        |   |   `-- SchemaManager.java
        |   |-- debug/
        |   |   |-- IdentityDiscoveryWatcher.java
        |   |   |-- IdentityLifecycleBridge.java
        |   |   `-- IdentityTestHook.java
        |   |-- repo/
        |   |   |-- PostgresAccountRepository.java
        |   |   |-- PostgresOwnershipRepository.java
        |   |   |-- PostgresPermissionRepository.java
        |   |   `-- PostgresRoleRepository.java
        |   `-- service/
        |       |-- AccountService.java
        |       |-- AuthSelfTestService.java
        |       |-- BankService.java
        |       |-- BootstrapAdminService.java
        |       |-- IdentityLifecycleService.java
        |       |-- IdentityLifecycleState.java
        |       |-- OwnershipService.java
        |       |-- PermissionService.java
        |       |-- PlayerContextResolver.java
        |       |-- PlayerIdentity.java
        |       |-- PlayerIdentityService.java
        |       |-- PzPlayerIdentityAdapter.java
        |       |-- PzRuntimeConfig.java
        |       `-- RoleService.java
        |
        |-- main/resources/sql/
        |   |-- accounts/
        |   |   |-- 001_schema.sql
        |   |   |-- 002_accounts.sql
        |   |   `-- 004_account_identity_columns.sql
        |   |-- economy/
        |   |   |-- 001_schema.sql
        |   |   |-- 002_wallet.sql
        |   |   `-- 003_transactions.sql
        |   |-- ownership/
        |   |   |-- 001_schema.sql
        |   |   |-- 002_nodes.sql
        |   |   `-- 003_account_node.sql
        |   `-- permissions/
        |       |-- 001_schema.sql
        |       |-- 002_account_permissions.sql
        |       `-- 003_account_nodes.sql
        |
        `-- test/java/pirch/pz/service/
            `-- PlayerIdentityServiceTest.java
```

---

## Modules

## 1) PirchsPZAgent

### Purpose

This is the JVM entrypoint for the runtime.

### What it does

- starts from `-javaagent`
- resolves the mod `lib` directory
- supports override input through agent args, properties, and env vars
- loads jars from the mod library folder
- reflects into the configured bootstrap class
- starts the Java-side runtime before normal gameplay-side use

### Why it matters

This is what lets the backend live in Java without requiring the runtime to be manually wired up inside ordinary gameplay code.

---

## 2) PirchsPZLoader

### Purpose

This is the runtime core.

It owns the generic bridge plumbing rather than gameplay logic.

### Responsibilities

- registering methods
- storing metadata per method
- validating minimum argument counts
- dispatching invocations
- returning structured results

### Key runtime classes

#### `BridgeMethodDefinition`
Stores metadata for each method:

- method name
- version
- description
- minimum argument count

#### `BridgeRequest`
Encapsulates an invocation request.

#### `ModuleRegistry`
Stores the registered methods.

#### `InvocationDispatcher`
Handles dispatch and validation.

#### `BridgeResult`
Standard success/failure result wrapper returned from bridge calls.

### Why it matters

The loader is not just a string-to-method map anymore.
It is a proper runtime registry with enough structure to support health checks, method discovery, and safer expansion later.

---

## 3) PirchsPZBridge

### Purpose

This is the actual backend layer.

It contains the bridge endpoints, schema bootstrap, identity logic, lifecycle handling, repositories, and gameplay-facing services.

### Current bootstrap flow

`BridgeBootstrap.initialize()` currently does the following:

1. guards against double initialization
2. initializes the loader
3. initializes schema
4. registers system bridge methods
5. registers bank bridge methods
6. registers ownership bridge methods
7. registers permissions bridge methods
8. logs a runtime summary
9. marks lifecycle readiness
10. starts the Java-side identity discovery watcher

### Important note

The bridge now logs ownership/permissions activation and the self-test path during startup. That is a meaningful change from the earlier README and reflects that authorization is now part of the actual runtime surface.

---

## Bridge API

The current public bridge surface is now wider than the previous README described.

## System

### `pz.bridge.system.ping`
Returns `pong`.

### `pz.bridge.system.version`
Returns the current bridge version string.

Current value:

```text
PirchsPZBridge v0.0.3
```

### `pz.bridge.system.healthCheck`
Returns a runtime summary including:

- bridge version
- registered method count
- whether major bridge subsystems are present
- lifecycle snapshot
- auth self-test status

### `pz.bridge.system.listMethods`
Returns all registered bridge methods and metadata.

### `pz.bridge.system.dbPing`
Attempts a real JDBC connection and returns:

- `db-ok` on success
- failure text on error

### `pz.bridge.system.resolveAccount`
Accepts a structured identity payload or legacy string input and resolves or creates an account.

### `pz.bridge.player.resolveIdentity`
Normalizes a structured identity payload into canonical identity output.

### `pz.bridge.player.getLifecycleState`
Returns lifecycle state plus the last resolved identity/account information if present.

### `pz.bridge.system.runAuthSelfTest`
Runs the Java-side ownership/permissions self-test against the currently resolved local account.

### `pz.bridge.system.getAuthSelfTestStatus`
Returns current self-test status and the last result snapshot.

## Bank

### `pz.bridge.bank.getBalance`
Returns the wallet balance for a resolved identity.

### `pz.bridge.bank.deposit`
Resolves identity, validates amount, and deposits funds.

### `pz.bridge.bank.withdraw`
Resolves identity, validates amount, checks balance, and withdraws funds.

## Ownership

### `pz.bridge.ownership.claimNode`
Claims a world node for an account.

### `pz.bridge.ownership.releaseNode`
Releases a claimed node.

### `pz.bridge.ownership.getNodeOwner`
Returns the current owner of a node.

### `pz.bridge.ownership.listOwnedNodes`
Returns the list of nodes currently owned by an account.

### `pz.bridge.ownership.isOwner`
Checks whether an identity currently owns a given node.

## Permissions

### `pz.bridge.permissions.grant`
Grants a permission to a target account.

### `pz.bridge.permissions.revoke`
Revokes a permission from a target account.

### `pz.bridge.permissions.has`
Checks whether an account has a permission, including owner-implied scoped access where applicable.

### `pz.bridge.permissions.list`
Lists active permission grants for an account.

### `pz.bridge.permissions.explain`
Returns an explanation of why access is granted or denied for a permission/scope combination.

---

## Identity Model

One of the most important shifts in this repo is the move away from loose string handling toward a proper structured identity model.

## `PlayerIdentity`

The current identity object holds:

- `playerSource`
- `sourcePlayerId`
- `steamId`
- `onlineId`
- `username`
- `displayName`
- `characterForename`
- `characterSurname`
- `characterFullName`
- `accountExternalId`
- `characterExternalId`

### Canonical account identity selection

Account identity currently resolves in this order:

1. `steam:<steamId>`
2. `user:<username>`
3. `online:<onlineId>`
4. `<playerSource>:<sourcePlayerId>`

That means Steam is now the highest-priority account anchor when available.

### Character identity shape

Character identity is currently derived from the account external ID plus a slugified character name:

```text
steam:76561198000000000#char:frank-west
```

This is important because the codebase is now clearly separating account identity from character identity, even though the deeper account/character model is still in-progress.

### Preferred naming helpers

#### Preferred account name order

1. `displayName`
2. `username`
3. `accountExternalId`

#### Preferred character name order

1. `characterFullName`
2. `characterForename`
3. preferred account name

---

## Project Zomboid identity extraction

## `PzPlayerIdentityAdapter`

This class turns `IsoPlayer` into a structured `PlayerIdentity`.

Current behavior includes:

- reading `player.getSteamID()`
- falling back to `SteamUser.GetSteamIDString()` where needed
- reading online ID
- reading username / display name
- attempting to resolve descriptor-backed character names
- building best-effort account and character identity payloads

## `PlayerContextResolver`

This helper tries to find a usable `IsoPlayer` from live game context.

## `IdentityDiscoveryWatcher`

The bridge starts a Java-side watcher that looks for a usable player context and feeds it into the lifecycle path.

## `IdentityLifecycleService`

This service now owns the session-aware promotion path for the local player.

It currently handles:

- ready / not-ready lifecycle state
- resetting local resolution when sessions change
- resolving account identity from live player context
- preventing duplicate processing of the same resolved account
- storing a snapshot of the last resolved local account
- triggering the auth self-test after local account resolution

This is a more formal direction than the earlier debug-only lifecycle setup.

---

## Ownership and Permissions Model

This is one of the biggest practical upgrades in the current repo.

The backend now has a generic ownership and authorization foundation rather than just identity + economy.

## Ownership answers

```text
Who owns this node?
```

## Permissions answer

```text
What is this identity allowed to do?
```

### Example permission keys

- `ui.mechanic.open`
- `ui.police.open`
- `business.manage`
- `ownership.manage`
- `permissions.manage`
- `permissions.manage.scope`

### Scope shape

Permissions can be:

- **global**
  - `scope_type = NULL`
  - `scope_key = NULL`

- **scoped**
  - example: `scope_type = node`
  - example: `scope_key = business:rusty_wrench_shop`

### Owner-implied access

For node-scoped access, owners are treated as implicitly allowed for a limited set of permissions, including:

- `ownership.use`
- `ownership.manage`
- `ownership.release`
- `ownership.transfer`
- `permissions.manage.scope`
- `ui.node.manage`

That matters because it means an owner can manage access to their own node without needing a globally privileged account.

### Design direction

This system stays generic on purpose.
It does not hardcode jobs like mechanic or police into the backend.
The backend cares about permission keys and scopes.
Higher-level job systems can map onto those keys later.

---

## Database Layer

## Current database choice

The repo currently uses:

- **PostgreSQL JDBC**
- driver: `org.postgresql:postgresql:42.7.3`

This is a PostgreSQL-first implementation today.

## `DatabaseConfig`
Loads `pirchdb.properties` from the classpath and exposes database settings.

## `DatabaseManager`
Creates JDBC connections using the configured PostgreSQL settings.

## `SchemaManager`
Runs known SQL files from the configured schema folders during startup.

### Current schema areas

- `accounts`
- `economy`
- `ownership`
- `permissions`

### Important schema files currently relevant

#### Accounts
- `001_schema.sql`
- `002_accounts.sql`
- `004_account_identity_columns.sql`

#### Economy
- `001_schema.sql`
- `002_wallet.sql`
- `003_transactions.sql`

#### Ownership
- `001_schema.sql`
- `002_nodes.sql`
- `003_account_node.sql`

#### Permissions
- `001_schema.sql`
- `002_account_permissions.sql`
- `003_account_nodes.sql`

### Important implementation note

The schema bootstrap is still driven by a fixed known-file list in `SchemaManager`.
That means schema growth still needs to stay aligned with that list until a fuller migration/versioning pass is added.

### Persistence responsibilities now present

The repository layer is currently handling:

- account resolve/create
- account identity refresh / last-seen tracking
- wallet ensure / read / update
- transaction insert logic
- node claim / release / owner lookup
- permission grant / revoke / list / explain
- role foundation used by bootstrap-admin setup

---

## Build and Install

## Root project configuration

The root build currently applies to subprojects:

- group: `pirch`
- version: `0.0.3`
- Java toolchain: **25**
- Maven Central for dependencies
- JUnit Platform for tests

### Included modules

- `PirchsPZLoader`
- `PirchsPZBridge`
- `PirchsPZAgent`

### Current root project name

`settings.gradle` still sets:

```text
rootProject.name = 'Zomboid'
```

So the Gradle project name is still `Zomboid` even though the repository name is `pirchs-pz-dbi`.

## Build commands

From the repo root:

```bash
gradle build
```

Or on Windows with the wrapper:

```powershell
.\gradlew.bat build
```

## Environment check

The root build includes a `verifyEnv` task.

Run it with:

```powershell
.\gradlew.bat verifyEnv
```

That prints the currently resolved Java toolchain target, Project Zomboid compile path, and mod `lib` target.

## `installToMod`

The root project defines an `installToMod` task.

It currently:

- builds `PirchsPZLoader`
- builds `PirchsPZBridge`
- builds `PirchsPZAgent`
- copies all three jars into the resolved mod `lib` folder
- copies the PostgreSQL runtime dependency jar into the same folder

### Example

```powershell
.\gradlew.bat installToMod
```

Or with an explicit mod lib override:

```powershell
.\gradlew.bat installToMod -PpzModLibDir="C:/Users/ryanj/Zomboid/mods/PirchsPZDBI/42/lib"
```

---

## Compile-time Project Zomboid classpath

`PirchsPZBridge/build.gradle` now resolves the Project Zomboid install directory from:

1. `-PpzLibDir=...`
2. `PZ_LIB_DIR`
3. `-PpzGameDir=...`
4. `PZ_GAME_DIR`
5. fallback to the default Steam install path

If that path exists, the bridge compiles against:

- `projectzomboid.jar`
- top-level jars in the game directory
- jars under the nested `lib/` folder

This is what enables compile-time references to classes like `IsoPlayer`.

---

## Runtime Boot Sequence

### Step 1: Build the jars

Build the modules from the root project.

### Step 2: Copy runtime jars into the mod `lib` folder

The loader jar, bridge jar, agent jar, and PostgreSQL driver jar need to be present in the Project Zomboid mod `lib` folder.

### Step 3: Launch with `-javaagent`

General shape:

```text
-javaagent:path\to\PirchsPZAgent.jar
```

### Step 4: Optional overrides

The mod library path can be overridden with agent args, system properties, or environment variables.

Example:

```text
-javaagent:path\to\PirchsPZAgent.jar=modLib=C:/Users/ryanj/Zomboid/mods/PirchsPZDBI/42/lib
```

### Step 5: Agent invokes bootstrap

The agent reflects into:

```text
pirch.pz.BridgeBootstrap.initialize()
```

### Step 6: Bridge initializes runtime systems

The bridge initializes schema, registers runtime methods, marks lifecycle readiness, and starts the Java-side identity watcher.

---

## Configuration

The bridge expects a classpath resource named:

```text
pirchdb.properties
```

A typical PostgreSQL section looks like this:

```properties
pirchdb.enabled=true
pirchdb.type=postgres
pirchdb.host=127.0.0.1
pirchdb.port=5432
pirchdb.name=pzlife
pirchdb.user=postgres
pirchdb.password=postgres
pirchdb.ssl=false
pirchdb.schema.auto_init=true
pirchdb.schema.locations=sql/accounts,sql/economy,sql/ownership,sql/permissions
```

If `pirchdb.url` is provided, it overrides the constructed JDBC URL.

### Runtime config keys now supported

The runtime config helper also supports additional properties for detector/auth behavior, including:

```properties
identity.detector.enabled=true
identity.detector.poll_ms=3000
identity.detector.max_attempts=0
identity.detector.log_every_attempts=10
identity.logging.verbose=false
identity.session.monitoring.enabled=true
identity.session.empty_checks_to_rearm=3

auth.selftest.enabled=true
auth.selftest.node_key=test:node1
auth.selftest.node_type=node
auth.selftest.permission_key=business.manage
auth.selftest.scope_type=node
auth.selftest.scope_key=test:node1
auth.selftest.teardown=false

auth.bootstrap_admin.enabled=true
auth.bootstrap_admin.external_id=bootstrap:admin
auth.bootstrap_admin.role_key=admin
auth.bootstrap_admin.display_name=Bootstrap Admin
```

### Local development convenience files

The repo now includes:

- `gradle.properties.example`
- `local.dev.properties.example`

The local example file is intended to make it easier to standardize:

- Java version
- Project Zomboid install path
- mod `lib` output path

without hardcoding everything directly into commands every time.

---

## Development Notes

## General extension pattern

The intended pattern in this repo is still:

```text
Bridge -> Service -> Repository -> Database
```

That remains the right way to add new systems.

## Adding a new bridge method

General pattern:

1. add or update a bridge class in `PirchsPZBridge`
2. register the method through `ModuleRegistry.register(...)`
3. define useful metadata with `BridgeMethodDefinition.builder(...)`
4. place business logic in a service class
5. keep persistence logic in a repository class
6. return `BridgeResult.ok(...)` or `BridgeResult.fail(...)`
7. make sure registration is reached from `BridgeBootstrap.initialize()`

## Recommended direction for future systems

This backend is already well-positioned for systems like:

- player banking
- world ownership
- businesses
- job menus
- admin panels
- persistent permissions
- account-backed service APIs

The important part is to keep those systems expressed in terms of stable identity and generic permission keys rather than hardcoded one-off checks.

---

## Testing

The repo now has an initial JUnit test file for identity behavior:

- `PlayerIdentityServiceTest`

Current tests cover:

- Steam-first account identity preference
- username fallback when Steam is absent
- legacy input compatibility

This is not a large test suite yet, but it is a real step up from the previous state where the repo effectively had no visible automated coverage.

Run tests with:

```powershell
.\gradlew.bat test
```

---

## Known Gaps / Next Pass

This repo has moved well beyond a skeleton, but there are still rough edges and next-pass items.

### Current rough edges

- hardcoded Windows fallback paths still exist
- schema bootstrap still relies on a fixed known-file list
- migration/version tracking is still not formalized
- some lifecycle behavior is still heavily log-driven
- the account vs. character model is clearer now, but still not fully expanded into separate first-class domain entities
- bootstrap-admin / role scaffolding exists, but the broader role model is still early

### Realistic next steps

- formalize migrations instead of fixed bootstrap file ordering
- expand test coverage around repositories and permissions behavior
- finish the next-pass auth architecture work
- make Lua or UI-side integration consume the Java bridge more formally
- continue reducing debug-path assumptions as gameplay hooks become more concrete
- decide how far role-based authorization should go compared to permission-only flows

---

## Maintainer Notes

## Fastest correct mental model

The clean mental model for this repo is:

- **Agent** starts before normal code
- **Loader** owns registration and dispatch
- **Bridge** exposes callable backend methods
- **Identity** turns live player context into stable account and character IDs
- **Services** implement the rules
- **Repositories** persist the state
- **PostgreSQL** is the actual backing store

## Most important strings in the repo right now

- bootstrap class:
  - `pirch.pz.BridgeBootstrap`
- current bridge version:
  - `PirchsPZBridge v0.0.3`
- default mod lib path:
  - `C:/Users/ryanj/Zomboid/mods/PirchsPZDBI/42/lib`
- default Project Zomboid path:
  - `C:/Program Files (x86)/Steam/steamapps/common/ProjectZomboid`

## In plain English

This repo now gives you a working Java-side base for:

- injecting a runtime into Project Zomboid
- loading mod-side jars
- registering named Java bridge methods
- resolving stable player/account identity from live game objects
- persisting account, wallet, ownership, and authorization data in PostgreSQL
- building larger persistent systems on top of that foundation

That makes this repo the backend platform layer for systems like:

- banking
- economy
- ownership
- permissions
- service-backed menus
- long-lived account systems

---

<div align="center">

### Pirchs PZ DBI

</div>
