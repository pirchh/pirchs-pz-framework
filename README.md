# Pirchs PZ DBI

<div align="center">

# Pirchs PZ DBI
### Java runtime, bridge, and database-backed identity/economy integration for Project Zomboid

> A multi-module Java runtime for injecting backend services into the Project Zomboid JVM, registering bridge methods, resolving player identity, and persisting account and banking data in PostgreSQL.

![Java](https://img.shields.io/badge/Java-25-blue?style=for-the-badge)
![Gradle](https://img.shields.io/badge/Gradle-Multi--Project-02303A?style=for-the-badge&logo=gradle)
![Project Zomboid](https://img.shields.io/badge/Project%20Zomboid-Modding-6D8E23?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Prototype-orange?style=for-the-badge)
![Database](https://img.shields.io/badge/Database-PostgreSQL-336791?style=for-the-badge&logo=postgresql)

</div>

---

## Table of Contents

- [What This Repo Is](#what-this-repo-is)
- [What Changed Recently](#what-changed-recently)
- [Current Architecture](#current-architecture)
- [Repo Layout](#repo-layout)
- [Modules](#modules)
- [Current Registered Bridge Methods](#current-registered-bridge-methods)
- [Identity Model](#identity-model)
- [Database Layer](#database-layer)
- [Build and Install](#build-and-install)
- [How the Runtime Boots](#how-the-runtime-boots)
- [Configuration](#configuration)
- [Development Notes](#development-notes)
- [Known Current State](#known-current-state)
- [Git Workflow](#git-workflow)
- [Maintainer Notes](#maintainer-notes)

---

## What This Repo Is

**Pirchs PZ DBI** is a **Gradle multi-project Java runtime** for Project Zomboid.

Despite the repo name, this is not just a tiny database helper. The repository currently contains three coordinated modules:

1. **PirchsPZAgent**  
   A Java agent that starts before regular game code, resolves the mod library path, loads jars from the mod `lib` folder, and invokes the runtime bootstrap class.

2. **PirchsPZLoader**  
   The runtime core that provides initialization, method registration, method metadata, dispatch, validation, and structured bridge results.

3. **PirchsPZBridge**  
   The actual bridge and service layer. This is where system methods, banking methods, PostgreSQL config, schema bootstrap, identity resolution, and repository/service logic live.

Today, the implementation is explicitly **PostgreSQL-backed**, not SQLite-backed.

---

## What Changed Recently

This README reflects the current codebase after the latest identity-focused updates.

### Major additions now present in the repo

- **Steam-backed identity foundation**
- **Structured `PlayerIdentity` model**
- **Project Zomboid-side identity adapter using `IsoPlayer` and `SteamUser`**
- **Canonical external ID generation**
- **Metadata-aware bridge method registration**
- **New request object for invocation dispatch**
- **Bridge method listing endpoint**
- **Java-side identity discovery watcher**
- **Identity lifecycle debug bridge**
- **Expanded account schema fields for last-seen identity data**
- **`installToMod` now copies the agent jar too**
- **Build version bumped to `0.0.2`**
- **Java toolchain moved to `25`**

### Important practical effect

The repo is no longer just "resolve a string external id and move money around."

It now has the beginnings of a real identity pipeline:

```text
IsoPlayer / Steam / online player context
        ->
PlayerIdentity
        ->
canonicalExternalId
        ->
account resolution / account creation
        ->
wallet + transaction persistence
```

---

## Current Architecture

```text
Project Zomboid JVM
        |
        v
PirchsPZAgent
  - premain()
  - resolves mod lib path
  - parses agent args / property / env overrides
  - loads jars from mod lib folder
  - reflects into pirch.pz.BridgeBootstrap
        |
        v
PirchsPZBridge
  - initializes loader
  - initializes schema
  - registers system bridge methods
  - registers bank bridge methods
  - arms Java-side identity discovery
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
Database / Services / Identity
  - DatabaseConfig
  - DatabaseManager
  - SchemaManager
  - AccountService
  - BankService
  - PlayerIdentity
  - PlayerIdentityService
  - PlayerContextResolver
  - PzPlayerIdentityAdapter
  - PostgresAccountRepository
        |
        v
PostgreSQL
```

---

## Repo Layout

```text
pirchs-pz-dbi/
|
|-- build.gradle
|-- settings.gradle
|-- gradlew
|-- gradlew.bat
|-- gradle/wrapper/
|-- README.md
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
    `-- src/main/
        |-- java/pirch/pz/
        |   |-- BridgeBootstrap.java
        |   |-- bridge/
        |   |   |-- BankBridge.java
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
        |   |   `-- PostgresAccountRepository.java
        |   `-- service/
        |       |-- AccountService.java
        |       |-- BankService.java
        |       |-- PlayerContextResolver.java
        |       |-- PlayerIdentity.java
        |       |-- PlayerIdentityService.java
        |       `-- PzPlayerIdentityAdapter.java
        `-- resources/
            `-- sql/
                |-- accounts/
                |   |-- 001_schema.sql
                |   |-- 002_accounts.sql
                |   |-- 003_player_identity.sql
                |   `-- 004_account_identity_columns.sql
                |-- economy/
                |   |-- 002_wallet.sql
                |   `-- 003_transactions.sql
                |-- ownership/
                |   `-- 002_nodes.sql
                `-- permissions/
                    `-- 003_account_nodes.sql
```

---

## Modules

## 1) PirchsPZAgent

### Purpose

This module is the **JVM entrypoint** for the runtime.

### What it currently does

- defines the agent startup entrypoint
- parses agent args
- resolves the mod lib path from:
  - `modLib=...` agent arg
  - `pirch.pz.modLib` system property
  - `PIRCHS_PZ_MOD_LIB` environment variable
  - fallback hardcoded Windows path
- optionally resolves a custom bootstrap class
- loads jars from the mod library folder
- reflects into the bridge bootstrap and invokes `initialize()`

### Default assumptions

Current defaults in code:

- default mod lib:
  - `C:/Users/ryanj/Zomboid/mods/PirchsPZDBI/42/lib`
- default bootstrap class:
  - `pirch.pz.BridgeBootstrap`

### Why this matters

The recent update made the agent more flexible than the original README described. You are no longer locked to only the hardcoded mod path if you pass a property, env var, or agent arg.

---

## 2) PirchsPZLoader

### Purpose

This module is the **runtime core**.

It does not contain gameplay-specific banking or Project Zomboid identity extraction logic. Instead, it provides the bridge plumbing:

- registration
- dispatch
- metadata
- argument count validation
- structured response handling

### Important classes

#### `BridgeMethodDefinition`
Defines metadata for a registered bridge method:

- `methodName`
- `version`
- `description`
- `minArgCount`

#### `BridgeRequest`
Encapsulates an invocation request:

- target method name
- argument array
- argument count

#### `ModuleRegistry`
Stores registered methods and their metadata.

#### `InvocationDispatcher`
Accepts either:
- `invoke(String methodName, Object... args)`
- `invoke(BridgeRequest request)`

It validates method presence and minimum argument count before executing the handler.

#### `BridgeResult`
The structured result wrapper returned from bridge calls.

### Why this matters

The loader is no longer just a string-to-function map. It now supports enough metadata to expose method discovery and safer dispatch behavior.

---

## 3) PirchsPZBridge

### Purpose

This module contains the **bridge logic, identity layer, account resolution, banking behavior, schema bootstrap, and database access**.

This is where the runtime becomes useful to actual gameplay systems.

### Current bootstrap flow

`BridgeBootstrap.initialize()` currently does the following:

1. guards against double initialization
2. initializes the loader
3. initializes database schemas
4. registers system bridge methods
5. registers bank bridge methods
6. logs registration summary
7. marks the identity lifecycle system as ready
8. starts the Java-side identity discovery watcher

### Key point

The current bridge no longer does the old startup smoke-test sequence documented in the existing README. It now focuses on method registration summary and identity discovery startup.

---

## Current Registered Bridge Methods

These are the bridge methods currently registered in code.

### System methods

- `pz.bridge.system.ping`
- `pz.bridge.system.version`
- `pz.bridge.system.healthCheck`
- `pz.bridge.system.listMethods`
- `pz.bridge.system.dbPing`
- `pz.bridge.system.resolveAccount`
- `pz.bridge.player.resolveIdentity`

### Bank methods

- `pz.bridge.bank.getBalance`
- `pz.bridge.bank.deposit`
- `pz.bridge.bank.withdraw`

### What they do

#### `pz.bridge.system.ping`
Returns:

```text
pong
```

#### `pz.bridge.system.version`
Returns the current bridge version string.

Current value in code:

```text
PirchsPZBridge v0.0.2
```

#### `pz.bridge.system.healthCheck`
Returns a runtime summary including:
- bridge version
- registered method count
- presence checks for major endpoints

#### `pz.bridge.system.listMethods`
Returns a list of registered bridge methods and their metadata:
- method name
- version
- minimum argument count
- description

#### `pz.bridge.system.dbPing`
Attempts a real JDBC connection and returns:
- `db-ok` on success
- failure text if connection setup fails

#### `pz.bridge.system.resolveAccount`
Accepts either:
- a structured `PlayerIdentity`
- a map-like bridge payload
- a legacy external-id style argument

Resolves or creates an account and returns:
- `accountId`
- normalized identity fields
- `accountName`

#### `pz.bridge.player.resolveIdentity`
Normalizes a structured bridge argument into canonical player identity data.

#### `pz.bridge.bank.getBalance`
Returns the wallet balance for a resolved identity.

#### `pz.bridge.bank.deposit`
Validates integer amount, resolves identity, and deposits funds.

#### `pz.bridge.bank.withdraw`
Validates integer amount, resolves identity, checks funds, and withdraws funds.

---

## Identity Model

One of the biggest changes in the repo is the move from loose string identifiers toward a structured identity model.

## `PlayerIdentity`

The current identity object can hold:

- `playerSource`
- `sourcePlayerId`
- `steamId`
- `onlineId`
- `username`
- `displayName`
- `characterForename`
- `characterSurname`
- `characterFullName`

### Canonical identity selection

`PlayerIdentity.getCanonicalExternalId()` currently prefers identifiers in this order:

1. `steamId`
2. `user:<username>`
3. `online:<onlineId>`
4. `<playerSource>:<sourcePlayerId>`

That means Steam is now the highest-priority identity anchor when available.

### Preferred account naming

`PlayerIdentity.getPreferredAccountName()` currently prefers:

1. `displayName`
2. `username`
3. canonical external id

### Preferred character naming

`PlayerIdentity.getPreferredCharacterName()` currently prefers:

1. `characterFullName`
2. `characterForename`
3. preferred account name

---

## Project Zomboid identity extraction

## `PzPlayerIdentityAdapter`

This class builds a `PlayerIdentity` from `IsoPlayer`.

Current behavior includes:

- reading `player.getSteamID()`
- falling back to `SteamUser.GetSteamIDString()`
- reading `player.getOnlineID()`
- reading username and display name
- attempting to resolve character forename/surname through descriptor methods
- generating a best-effort full character name
- assigning a source type such as:
  - `steam`
  - `username`
  - `online`
  - `unknown`

## `PlayerContextResolver`

This helper tries to find a usable `IsoPlayer` from the current game context.

Current behavior includes:

- trying `IsoPlayer.getInstance()`
- trying `IsoPlayer.getPlayers()`
- preferring a local player when available
- resolving from an online ID when explicitly provided

## Java-side discovery watcher

## `IdentityDiscoveryWatcher`

The bridge now starts a background watcher thread that:

- polls roughly once per second
- stops after a maximum attempt count
- tries to locate a visible `IsoPlayer`
- hands the found player to the identity lifecycle bridge

## `IdentityLifecycleBridge`

This debug/lifecycle helper currently:

- waits until the bridge is marked ready
- converts `IsoPlayer` into `PlayerIdentity`
- avoids re-processing the same canonical identity repeatedly
- resolves or creates an account
- logs recommended database key, account name, and character name

This is clearly still an early-stage discovery/debug path, but it is important enough that the README should document it.

---

## Database Layer

## Important note

Although earlier discussions included SQLite ideas, the code in this repo currently uses:

- **PostgreSQL JDBC**
- driver dependency:
  - `org.postgresql:postgresql:42.7.3`

## `DatabaseConfig`
Loads `pirchdb.properties` from the classpath and exposes database settings.

## `DatabaseManager`
Creates JDBC connections using the configured PostgreSQL settings.

## `SchemaManager`
Runs SQL resources from configured schema folders during startup.

### Known files currently executed by `SchemaManager`

- `001_schema.sql`
- `002_accounts.sql`
- `002_wallet.sql`
- `002_nodes.sql`
- `003_transactions.sql`
- `003_account_nodes.sql`
- `004_account_identity_columns.sql`

### Important note about schema files

The repo also contains:

- `sql/accounts/003_player_identity.sql`

But `SchemaManager` does **not** currently include that filename in its known-file list.

So as of the current codebase:

- the file exists in the repo
- but it is not part of the automatic schema bootstrap sequence

That is worth knowing before assuming all identity SQL files are auto-applied.

---

## Service and Repository Layer

## `AccountService`
Thin service layer for account resolution.

Now supports resolving accounts from structured `PlayerIdentity`, not just legacy external IDs.

## `BankService`
Thin service layer for:

- get balance
- deposit
- withdraw

It now accepts `PlayerIdentity` inputs in the current flow.

## `PostgresAccountRepository`

This is where the main persistence logic currently lives.

### Current responsibilities

- resolve or create account by external id
- resolve or create account by `PlayerIdentity`
- create account rows with identity metadata
- touch/update account identity fields
- track last-seen username/display name/online id
- track last-seen character name fields
- ensure wallet existence
- fetch balance
- deposit funds
- withdraw funds
- insert transaction records

### Current account fields referenced in code

The repository now works with identity-related columns such as:

- `external_id`
- `canonical_external_id`
- `player_source`
- `source_player_id`
- `steam_id`
- `account_name`
- `username_last_seen`
- `display_name_last_seen`
- `online_id_last_seen`
- `character_forename_last_seen`
- `character_surname_last_seen`
- `character_full_name_last_seen`
- `identity_last_seen_at`
- `last_seen_at`

### Current database areas referenced

- `accounts.account`
- `economy.wallet`
- `economy.transactions`

### Transaction behavior

The repository uses JDBC transactions for:

- account resolve/create flow
- deposit flow
- withdraw flow

That is good, because balance-changing logic is not being performed as loose one-off statements.

---

## Build and Install

## Root project configuration

The root project currently applies to subprojects:

- Java plugin
- group: `pirch`
- version: `0.0.2`
- Java toolchain: **25**
- repository: **Maven Central**

## Included modules

The Gradle settings currently include:

- `PirchsPZLoader`
- `PirchsPZBridge`
- `PirchsPZAgent`

### Note on root project name

`settings.gradle` currently sets:

```text
rootProject.name = 'Zomboid'
```

So the Gradle project name is currently `Zomboid`, even though the repository name is `pirchs-pz-dbi`.

## Build commands

From the repository root:

```bash
./gradlew build
```

On Windows PowerShell:

```powershell
.\gradlew.bat build
```

## `installToMod`

The root project defines an `installToMod` task.

It currently:

- builds `PirchsPZLoader`
- builds `PirchsPZBridge`
- builds `PirchsPZAgent`
- copies all three jars into the resolved mod lib directory
- copies the PostgreSQL runtime dependency jar into that same folder

This is a meaningful change from the previous README, which said the agent jar was **not** copied. That is no longer true.

### Run it on Windows

```powershell
.\gradlew.bat installToMod
```

## Mod lib resolution order

The install task and runtime flow can now resolve the mod lib directory from:

1. Gradle property:
   - `-PpzModLibDir=...`
2. environment variable:
   - `PIRCHS_PZ_MOD_LIB=...`
3. fallback:
   - `C:/Users/ryanj/Zomboid/mods/PirchsPZDBI/42/lib`

### Example

```powershell
.\gradlew.bat installToMod -PpzModLibDir="C:/Users/ryanj/Zomboid/mods/PirchsPZDBI/42/lib"
```

---

## Project Zomboid compile-time classpath

`PirchsPZBridge/build.gradle` now tries to resolve Project Zomboid jars from:

1. Gradle property:
   - `-PpzLibDir=...`
2. environment variable:
   - `PZ_LIB_DIR=...`
3. environment variable:
   - `PZ_GAME_DIR=...`
4. fallback:
   - `C:/Program Files (x86)/Steam/steamapps/common/ProjectZomboid`

If that directory exists, the bridge module compiles against:

- `projectzomboid.jar`
- other jars in that directory via `fileTree`

This is what enables the current code to reference Project Zomboid classes like `IsoPlayer` and `SteamUser`.

---

## How the Runtime Boots

### Step 1: Build jars

Build the modules with Gradle.

### Step 2: Put runtime jars into the mod lib folder

At minimum, the loader jar, bridge jar, agent jar, and PostgreSQL driver jar need to be available in the mod `lib` folder.

### Step 3: Launch with the Java agent

Your Java launch needs to include the agent jar with `-javaagent`.

General shape:

```text
-javaagent:path\to\PirchsPZAgent.jar
```

### Step 4: Optional runtime overrides

You can override the mod library path with either:

```text
-javaagent:path\to\PirchsPZAgent.jar=modLib=C:/Users/ryanj/Zomboid/mods/PirchsPZDBI/42/lib
```

or a JVM property / environment variable recognized by the agent.

You can also override the bootstrap class with:

```text
bootstrapClass=fully.qualified.ClassName
```

### Step 5: Agent invokes bridge bootstrap

The agent reflects into:

```text
pirch.pz.BridgeBootstrap.initialize()
```

That bootstrap initializes the loader, schema, bridge registrations, and identity discovery startup.

---

## Configuration

The bridge expects a classpath resource named:

```text
pirchdb.properties
```

Based on current code, a typical PostgreSQL config looks like this:

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

If you provide `pirchdb.url`, it overrides the constructed JDBC URL.

---

## Development Notes

## Where to add new callable methods

To expose a new bridge method:

1. create or update a bridge class in `PirchsPZBridge`
2. register it through `ModuleRegistry.register(...)`
3. define useful metadata with `BridgeMethodDefinition.builder(...)`
4. implement logic in a service and/or repository class
5. return `BridgeResult.ok(...)` or `BridgeResult.fail(...)`
6. ensure the registration path is called from `BridgeBootstrap.initialize()`

## Good current pattern

A good pattern in this repo is still:

```text
Bridge -> Service -> Repository -> Database
```

For identity-aware flows, the practical pattern now looks more like:

```text
Project Zomboid player context
        ->
PlayerIdentity
        ->
Bridge / Service
        ->
Repository
        ->
PostgreSQL
```

## Good next additions for this repo

Based on the current codebase, likely next steps would be:

- add a Lua-facing integration layer
- replace debug/discovery-only identity hooks with formal production event hooks
- add tests for identity normalization and repository behavior
- add migration/version tracking for SQL bootstrap
- remove remaining hardcoded Windows defaults where possible
- decide whether `003_player_identity.sql` should be executed automatically or removed
- formalize account/character separation if characters become first-class entities later

---

## Known Current State

This repo is already beyond a skeleton, but it is still early-stage and prototype-ish in a few places.

### What is already real

- multi-project Gradle setup
- Java 25 toolchain
- Java agent bootstrap
- dynamic mod-lib jar loading
- metadata-aware runtime registry
- structured bridge request/response model
- PostgreSQL JDBC integration
- schema auto-run support
- Project Zomboid compile-time integration
- Steam-backed identity resolution foundation
- canonical identity generation
- account resolution and wallet creation
- deposit and withdraw logic
- transaction inserts
- Java-side identity discovery watcher

### What is still prototype-level or rough

- hardcoded Windows fallback paths still exist
- no visible automated test suite in the repo
- identity discovery currently includes debug-oriented watcher behavior
- some runtime behavior is still heavily log-driven
- the repo name suggests a broad DB abstraction, but implementation is currently PostgreSQL-specific
- `settings.gradle` still uses `rootProject.name = 'Zomboid'`
- `003_player_identity.sql` exists but is not part of the automatic known-file bootstrap list

---

## Git Workflow

## File naming

The correct filename is:

```text
README.md
```

Use it exactly like that: all caps `README`, lowercase `.md`.

GitHub will automatically render it on the repository homepage.

## Safe workflow for this README update

From the repo root:

```powershell
git checkout -b docs/readme-refresh
git add README.md
git commit -m "docs: update README for current identity and runtime state"
git push -u origin docs/readme-refresh
```

Then open a PR and merge into `main`.

## Direct push to main

If you are intentionally committing straight to `main`:

```powershell
git add README.md
git commit -m "docs: update README for current identity and runtime state"
git push origin main
```

---

## Maintainer Notes

## What a new maintainer should understand first

The fastest correct mental model for this repo is:

- **Agent** starts before normal game code
- **Loader** owns registration and dispatch
- **Bridge** exposes callable methods
- **Identity layer** turns Project Zomboid player context into stable account keys
- **Repository** persists account/wallet state
- **PostgreSQL** is the actual backing store today

## The most important strings in the code right now

- bootstrap class:
  - `pirch.pz.BridgeBootstrap`
- current bridge version:
  - `PirchsPZBridge v0.0.2`
- default mod lib path:
  - `C:/Users/ryanj/Zomboid/mods/PirchsPZDBI/42/lib`
- default PZ install path:
  - `C:/Program Files (x86)/Steam/steamapps/common/ProjectZomboid`

## In plain English

This repo currently gives you a working base for:

- injecting a Java runtime into Project Zomboid
- loading mod-side jars
- registering named Java bridge methods
- normalizing player identity from live game objects
- resolving that identity into a stable account
- persisting wallet and transaction data in PostgreSQL

That is already enough groundwork for larger systems like:

- banking
- economy
- ownership
- permissions
- player-backed persistent services

---

<div align="center">

### Pirchs PZ DBI

</div>
