# Pirchs PZ DBI

<div align="center">

# Pirchs PZ DBI
### Java runtime, bridge, and database integration for Project Zomboid

> A multi-module Java runtime for injecting backend services into the Project Zomboid JVM, registering bridge methods, and exposing database-backed game systems to higher-level callers.

![Java](https://img.shields.io/badge/Java-17-blue?style=for-the-badge)
![Gradle](https://img.shields.io/badge/Gradle-Multi--Project-02303A?style=for-the-badge&logo=gradle)
![Project Zomboid](https://img.shields.io/badge/Project%20Zomboid-Modding-6D8E23?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Prototype-orange?style=for-the-badge)
![Database](https://img.shields.io/badge/Database-PostgreSQL-336791?style=for-the-badge&logo=postgresql)

</div>

---

## Table of Contents

- [What This Repo Is](#what-this-repo-is)
- [Current Architecture](#current-architecture)
- [Repo Layout](#repo-layout)
- [Modules](#modules)
- [Current Registered Bridge Methods](#current-registered-bridge-methods)
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

**Pirchs PZ DBI** is currently a **Gradle multi-project Java runtime** for Project Zomboid.

It is organized into three main modules:

1. **PirchsPZAgent**  
   A Java agent jar that starts before normal game code, loads jars from the mod `lib` folder, and invokes the bootstrap entrypoint for the bridge layer.

2. **PirchsPZLoader**  
   A lightweight runtime kernel that handles initialization, method registration, dispatch, structured bridge results, and loader logging.

3. **PirchsPZBridge**  
   The actual bridge and service layer. This is where system methods, banking methods, database config, schema bootstrap, and repository/service code live.

Even though the repo name says **DBI**, the code in the repo today is specifically wired for **PostgreSQL**, not SQLite. The runtime classpath also includes the PostgreSQL JDBC driver.

---

## Current Architecture

```text
Project Zomboid JVM
        |
        v
PirchsPZAgent
  - premain()
  - resolves mod lib path
  - loads jars from mod lib folder
  - reflects into pirch.pz.BridgeBootstrap
        |
        v
PirchsPZBridge
  - initializes loader
  - initializes schema
  - registers system bridge methods
  - registers bank bridge methods
  - runs startup test calls
        |
        v
PirchsPZLoader
  - ModuleRegistry
  - InvocationDispatcher
  - BridgeResult
  - Loader logging
        |
        v
Database / Services
  - DatabaseConfig
  - DatabaseManager
  - SchemaManager
  - AccountService
  - BankService
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
        |   |-- repo/
        |   |   `-- PostgresAccountRepository.java
        |   `-- service/
        |       |-- AccountService.java
        |       `-- BankService.java
        `-- resources/
            `-- sql/
                |-- accounts/
                |-- economy/
                |-- ownership/
                `-- permissions/
```

---

## Modules

## 1) PirchsPZAgent

### Purpose
This module is the **JVM entrypoint** for the runtime.

### What it currently does
- Defines the jar manifest with `Premain-Class: pirch.pzagent.AgentEntry`
- Accepts optional agent args
- Uses a default mod lib path if none is supplied
- Loads all jars from that folder using a `URLClassLoader`
- Reflectively calls:

```text
pirch.pz.BridgeBootstrap.initialize()
```

### Key files

#### `AgentEntry.java`
Main startup class for the agent.

Current behavior:
- logs startup
- parses `modLib=...` if passed as agent args
- falls back to a hardcoded default mod lib path:
  - `C:/Users/ryanj/Zomboid/mods/PirchsPZDBI/42/lib`
- loads jars from that folder
- loads bootstrap class:
  - `pirch.pz.BridgeBootstrap`
- invokes `initialize()` through reflection

#### `ModJarLoader.java`
Responsible for scanning the mod library folder and loading all `.jar` files.

Current behavior:
- fails if the folder does not exist
- fails if no jars are found
- sorts jars before loading
- uses `ClassLoader.getSystemClassLoader()` as parent

#### `AgentLog.java`
Very simple prefixed logger for stdout/stderr:
- info -> `[PirchsPZAgent]`
- error -> `[PirchsPZAgent]`

---

## 2) PirchsPZLoader

### Purpose
This module is the **runtime core**.

It does not know gameplay details. It provides the plumbing for:
- initialization state
- method registration
- method dispatch
- structured success/failure results
- loader-side logging

### Key files

#### `LoaderBootstrap.java`
Tracks whether the loader has been initialized.

Current behavior:
- guards against double initialization
- logs loader startup
- flips internal initialized state
- exposes `isInitialized()`

#### `ModuleRegistry.java`
Stores registered bridge methods.

Current behavior:
- registers bridge handlers by method name
- allows method lookup
- checks whether a method exists
- returns registered method count
- exposes an unmodifiable view of the registry
- logs registrations and overwrites

This is the heart of the bridge registration system.

#### `InvocationDispatcher.java`
Executes registered methods by string key.

Current behavior:
- resolves a handler from `ModuleRegistry`
- returns a structured failure if the method does not exist
- executes the method with `Object... args`
- wraps non-`BridgeResult` responses into `BridgeResult.ok(...)`
- logs success and failure
- catches exceptions and returns `BridgeResult.fail(...)`

#### `BridgeResult.java`
Simple structured response wrapper.

Shape:
- `success`
- `data`
- `error`

This is the main result object that bridge calls return today.

#### `LoaderLog.java`
Minimal prefixed logger:
- info -> `[PirchsPZLoader]`
- error -> `[PirchsPZLoader]`

---

## 3) PirchsPZBridge

### Purpose
This module contains the **actual bridge logic and game-adjacent service layer**.

This is where the repo becomes useful to gameplay systems:
- system calls
- account resolution
- wallet/balance handling
- deposits and withdrawals
- schema initialization
- JDBC access

### Bootstrap flow

`BridgeBootstrap.initialize()` currently does the following:

1. prevents double initialization
2. initializes the loader
3. initializes database schemas
4. registers system bridge methods
5. registers bank bridge methods
6. runs several startup test invocations and logs the results

### Startup self-calls currently performed
At startup the bridge immediately invokes:

- `pz.bridge.system.ping`
- `pz.bridge.system.dbPing`
- `pz.bridge.system.resolveAccount`
- `pz.bridge.bank.deposit`
- `pz.bridge.bank.getBalance`

That means the current runtime is already doing a small bootstrap smoke test once the bridge comes online.

---

## Current Registered Bridge Methods

These are the method names currently registered in code.

### System methods
- `pz.bridge.system.ping`
- `pz.bridge.system.version`
- `pz.bridge.system.healthCheck`
- `pz.bridge.system.selfTest`
- `pz.bridge.system.dbPing`
- `pz.bridge.system.resolveAccount`

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
PirchsPZBridge v0
```

#### `pz.bridge.system.healthCheck`
Returns a map containing:
- bridge version
- registered method count
- presence checks for key methods

#### `pz.bridge.system.selfTest`
Checks whether required methods are registered and returns:
- registration status booleans
- `passed = true/false`

#### `pz.bridge.system.dbPing`
Attempts a real JDBC connection and returns:
- `db-ok` on success
- a failure result if connection setup fails

#### `pz.bridge.system.resolveAccount`
Validates an external id and resolves or creates an account record.

Returns a map with:
- `accountId`
- `externalId`
- `accountName`

#### `pz.bridge.bank.getBalance`
Returns the player/account wallet balance.

#### `pz.bridge.bank.deposit`
Validates the amount and deposits funds into the wallet.

#### `pz.bridge.bank.withdraw`
Validates the amount, checks available funds, and withdraws funds.

---

## Database Layer

## Important note
Despite earlier discussion around SQLite, the code in this repo currently uses:

- **PostgreSQL JDBC**
- driver class: `org.postgresql.Driver`
- dependency: `org.postgresql:postgresql:42.7.3`

### `DatabaseConfig.java`
Loads `pirchdb.properties` from the classpath and exposes database settings.

Current supported settings include:
- `pirchdb.enabled`
- `pirchdb.type`
- `pirchdb.host`
- `pirchdb.port`
- `pirchdb.name`
- `pirchdb.user`
- `pirchdb.password`
- `pirchdb.ssl`
- `pirchdb.schema.auto_init`
- `pirchdb.schema.locations`
- `pirchdb.url`

Current assumptions:
- default db type is `postgres`
- unsupported db types throw an error
- defaults point to a PostgreSQL setup

### `DatabaseManager.java`
Creates JDBC connections.

Current behavior:
- checks whether DB access is enabled
- loads the PostgreSQL JDBC driver
- opens a connection with URL/user/password from config

### `SchemaManager.java`
Runs SQL files from classpath resources during startup.

Current behavior:
- only runs if DB is enabled and auto-init is enabled
- iterates configured schema folders
- attempts known filenames in each folder
- skips missing files silently
- executes non-empty SQL files

Known SQL filenames attempted:
- `001_schema.sql`
- `002_accounts.sql`
- `002_wallet.sql`
- `002_nodes.sql`
- `003_transactions.sql`
- `003_account_nodes.sql`

Configured default schema folders:
- `sql/accounts`
- `sql/economy`
- `sql/ownership`
- `sql/permissions`

---

## Service and Repository Layer

## `PostgresAccountRepository.java`
This is where the real persistence logic currently lives.

### Current responsibilities
- resolve or create account by `external_id`
- create account rows
- update account last seen / account name
- ensure wallet existence
- fetch balance
- deposit funds
- withdraw funds
- insert transaction records

### Current database areas referenced
- `accounts.account`
- `economy.wallet`
- `economy.transactions`

### Transaction behavior
The repository already uses JDBC transactions for:
- account resolve/create flow
- deposit flow
- withdraw flow

That is a good sign, because balance-changing operations are not being done as loose one-off statements.

## `AccountService.java`
Thin validation layer over account resolution.

## `BankService.java`
Thin validation layer over repository methods for:
- get balance
- deposit
- withdraw

### Current validation rules
- player/account identifiers cannot be null or blank
- deposit/withdraw amounts must be valid integers at the bridge layer
- repository rejects non-positive amounts
- withdraw rejects insufficient funds

---

## Build and Install

## Root project configuration

The root Gradle build currently applies to subprojects:
- Java plugin
- group: `pirch`
- version: `0.0.1`
- Java toolchain: **17**
- repository: **Maven Central**

### Included modules
The Gradle settings currently include:
- `PirchsPZLoader`
- `PirchsPZBridge`
- `PirchsPZAgent`

## Build commands

From the repository root:

```bash
gradlew build
```

On Windows PowerShell:

```powershell
.\gradlew.bat build
```

## Special install task

The root project defines:

```text
installToMod
```

This task currently:
- builds `PirchsPZLoader`
- builds `PirchsPZBridge`
- copies those jars into:
  - `C:/Users/ryanj/Zomboid/mods/PirchsPZDBI/42/lib`
- copies the PostgreSQL runtime dependency jar into that same folder

### Run it on Windows
```powershell
.\gradlew.bat installToMod
```

## Important note about the agent jar
The root `installToMod` task does **not** currently copy the agent jar.

So in practice, if you need the agent jar deployed too, you should build it and place it where your launch flow expects it.

A direct build for the agent module would be:

```powershell
.\gradlew.bat :PirchsPZAgent:build
```

---

## How the Runtime Boots

### Step 1: Build jars
Build the modules with Gradle.

### Step 2: Put runtime jars into the mod lib folder
At minimum, the bridge and loader jars need to be in the mod `lib` folder, plus the PostgreSQL driver jar.

### Step 3: Launch with the Java agent
Your Java launch needs to include the agent jar with `-javaagent`.

General shape:

```text
-javaagent:path\to\PirchsPZAgent.jar
```

If you want to explicitly override the mod lib path, the agent supports:

```text
-javaagent:path\to\PirchsPZAgent.jar=modLib=C:/Users/ryanj/Zomboid/mods/PirchsPZDBI/42/lib
```

### Step 4: Agent starts bridge bootstrap
The agent reflects into:

```text
pirch.pz.BridgeBootstrap.initialize()
```

That bootstrap initializes the loader, schema, bridges, and startup test calls.

---

## Configuration

The bridge expects a classpath resource named:

```text
pirchdb.properties
```

Based on current code, a typical config would look something like this:

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
2. register the method in `ModuleRegistry.register(...)`
3. implement logic in a service and/or repository class
4. return `BridgeResult.ok(...)` or `BridgeResult.fail(...)`
5. ensure the bridge registration is called from `BridgeBootstrap.initialize()`

## Suggested pattern
A good pattern in this repo is:

```text
Bridge -> Service -> Repository -> Database
```

That keeps:
- validation near the bridge/service edge
- SQL in the repository layer
- dispatch concerns in the loader layer

## Good next additions for this repo
Based on the current codebase, logical next steps would be:
- add a Lua-facing integration layer
- add more bridge domains beyond banking
- add richer health diagnostics
- add safer config handling for secrets
- add tests for repository and dispatcher behavior
- add explicit migration/version tracking for SQL bootstrap
- make mod lib path configurable without hardcoded user-specific defaults

---

## Known Current State

This repo is already more than a skeleton, but it is still early-stage.

### What is already real
- multi-project Gradle setup
- Java 17 toolchain
- Java agent bootstrap
- dynamic jar loading from mod lib
- central runtime registry and dispatcher
- structured bridge results
- PostgreSQL JDBC integration
- schema auto-run support
- account resolution flow
- wallet creation
- deposit and withdraw logic
- transaction inserts
- startup smoke testing

### What is still clearly prototype-level
- hardcoded Windows paths
- minimal logging
- no visible automated tests in repo
- version string still `PirchsPZBridge v0`
- config and deployment are still fairly manual
- root install task does not deploy the agent jar
- repo name suggests DB abstraction, but implementation is currently PostgreSQL-specific

---

## Git Workflow

## File naming
The correct filename is:

```text
README.md
```

Use it exactly like that. All caps `README`, lowercase `.md`.

That is the normal GitHub standard and GitHub will automatically render it on the repo homepage.

## Make a new commit

From the repo root:

```powershell
git status
git add README.md
git commit -m "docs: expand README with architecture and setup"
```

If you changed more than just the README:

```powershell
git add .
git commit -m "docs: expand README with architecture and setup"
```

## Push to main
If your branch is already `main` and already tracks origin:

```powershell
git push origin main
```

## If you are working on a feature branch first
Create and switch to a branch:

```powershell
git checkout -b docs/readme-refresh
```

Commit:

```powershell
git add README.md
git commit -m "docs: expand README with architecture and setup"
```

Push the branch:

```powershell
git push -u origin docs/readme-refresh
```

Then open a Pull Request on GitHub and merge it into `main`.

## Quick safest workflow I would recommend
For documentation changes, this is a clean flow:

```powershell
git checkout -b docs/readme-refresh
git add README.md
git commit -m "docs: expand project README"
git push -u origin docs/readme-refresh
```

Then merge via GitHub.

If you do not want a branch and just want to push straight to main:

```powershell
git add README.md
git commit -m "docs: expand project README"
git push origin main
```

---

## Maintainer Notes

## What a new maintainer should understand first
If someone new opens this repo, the first mental model should be:

- **Agent** starts the runtime
- **Loader** provides method registration and dispatch
- **Bridge** exposes callable functionality
- **Repository** performs SQL work
- **Database config** controls connection + schema bootstrap

## The most important strings in the code right now
- bootstrap class:
  - `pirch.pz.BridgeBootstrap`
- loader prefix:
  - `[PirchsPZLoader]`
- agent prefix:
  - `[PirchsPZAgent]`
- current bridge version:
  - `PirchsPZBridge v0`
- default mod lib path:
  - `C:/Users/ryanj/Zomboid/mods/PirchsPZDBI/42/lib`

## In plain English
This repo currently gives you a working path for:

- injecting a Java runtime into Project Zomboid
- loading bridge jars from a mod library folder
- registering string-based callable methods
- wiring those methods to real service and database logic
- persisting bank/account data in PostgreSQL

That is already a strong base for larger systems like:
- economy
- banking
- ownership
- permissions
- player-backed persistent services

---

<div align="center">

### Pirchs PZ DBI
**README generated to match the current codebase as closely as possible**

</div>
