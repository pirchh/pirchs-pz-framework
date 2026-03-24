# Stabilization And Auth Next Pass

This document describes the goal of the overwrite pack for the next DBI pass.

## Why this pass exists

The project is no longer at the point where the biggest risk is "missing features."
The bigger risk now is drift between:

- what the code boots
- what the schema defaults say
- what the health checks expose
- what the README claims
- what a maintainer expects after opening the repo

This pass is meant to reduce that drift.

## Main corrections in this overwrite pack

### 1. Roles are now treated as part of the active runtime surface

The runtime already registers `RoleBridge` and seeds role data during bootstrap.
This pass updates the docs and health reporting to treat roles as first-class runtime behavior rather than a future-only concern.

### 2. Default schema locations now include roles

One of the more important backend mismatches was that the runtime had role logic but the default schema location list did not include `sql/roles`.

That is corrected in:

- `DatabaseConfig.java`
- `pirchdb.properties.example`
- `README.md`

### 3. Schema bootstrap is less noisy and more intentional

The previous schema bootstrap approach swept a flat list of known files across every configured folder.
That worked, but it was not very clean.

This pass changes schema execution to:

- normalize configured folders
- use a per-folder file order where known
- fall back safely for unknown folders
- log what actually executed

### 4. Gradle defaults are less tied to one machine

The root build now prefers explicit local properties and environment variables.
If no mod lib path is configured, it falls back to `build/install/mod-lib` instead of assuming a user-specific Windows path.

### 5. Health reporting now exposes the auth/runtime shape more clearly

`SystemBridge.healthCheck()` now reports:

- roles registration presence
- schema auto-init status
- configured schema locations
- identity detector enablement
- bootstrap-admin enablement
- auth self-test enablement

That makes runtime inspection more useful when hardening the backend before new services.

## What should come after this pass

After this stabilization pass, the next sensible branch is probably one of these:

### Option A: Character/account split
Turn account and character into more explicit first-class domain entities.

### Option B: Property/business services
Build a higher-level service layer on top of the now-stabilized ownership, permissions, and roles foundation.

### Option C: Real migration/versioning
Replace the code-managed bootstrap ordering with a more formal migration story once the schema settles further.

## Suggested branch name

```text
feature/v0-0-3-stabilization-auth-slice
```

## Suggested commit title

```text
[Update] stabilize auth runtime
```
