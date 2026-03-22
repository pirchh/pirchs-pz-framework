# Java-side local-player detector revision

This patch keeps identity resolution on the Java side and replaces the previous startup-bound watcher behavior with a lower-frequency detector that is safer to leave armed until the player actually exists.

## What changed

- Replaced the old fixed-loop watcher behavior with a single-threaded scheduled detector.
- Default polling cadence changed from 1000 ms to 3000 ms.
- Default `max_attempts` changed to `0`, which means unlimited detection until success.
- Added `pirch.identity.watcher.log_every_attempts` so logs are quieter by default.
- Detector stops itself permanently after a successful identity/account resolution.
- Lifecycle service now exposes reset and snapshot-friendly helpers so the detector can be re-armed later if you decide to hook disconnect/world-unload events.

## Why this is safer

The new detector:

- uses one daemon thread
- does not retain `IsoPlayer` references between checks
- performs a tiny amount of work per tick
- stops immediately after successful resolution
- no longer depends on the player joining within a fixed bootstrap window by default

## Default behavior

- `poll_ms=3000`
- `max_attempts=0` meaning unlimited
- `log_every_attempts=10`

If you want the old bounded behavior back, set `pirch.identity.watcher.max_attempts` to a positive number.
