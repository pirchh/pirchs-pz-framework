# Implementation Notes

This drop-in patch moves the repo in four directions:

1. Identity is now explicitly split into **account identity** and **character identity**.
2. Lifecycle resolution is promoted to a first-class service and the watcher becomes a fallback bootstrap path.
3. Ownership and permissions are added as real verticals on top of the foundation.
4. Build and environment configuration are moved away from machine-hardcoded defaults.

## Identity model

`PlayerIdentity` now computes:
- `accountExternalId`
- `characterExternalId`

`canonicalExternalId` is still exposed for compatibility and points at `accountExternalId`.

The goal is that future systems key long-lived ownership, factions, money, and permissions to the account id, while still being able to attribute actions to a specific character identity when needed.

## Lifecycle model

`IdentityLifecycleService` is now the single place that:
- tracks readiness
- records last resolved identity
- suppresses duplicate resolution
- promotes a resolved `IsoPlayer` into account creation

`IdentityDiscoveryWatcher` is kept, but is now clearly a fallback poller driven by config properties from `pirchdb.properties`.

## Ownership and permissions

The new bridge verticals are intentionally small but useful:
- node ownership claims
- scoped permission grants
- permission checks

That gives you a clean next place to hang houses, businesses, containers, admin claims, and world objects.

## Merge approach

This zip is laid out as a direct overwrite pack:
- drag the top-level folders into your repo root
- let Windows merge/replace matching files
- then review the new files under `docs`, `src/test`, and the new bridge/service/repo classes


## Revision 2 build safety notes

- Default Java toolchain target is restored to 25 so existing local SDK setups keep building.
- Project Zomboid compile path now falls back to the original Steam install location when no override is provided.
- Bridge compile classpath now also includes `ProjectZomboid/lib/*.jar` in case the zombie classes live in nested jars on a local install.
- Recommended local override file remains `local.dev.properties` so machine-specific paths stay out of git.
