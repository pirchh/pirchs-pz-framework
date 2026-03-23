# Ownership + Permissions V1

This patch is intended to make ownership and permissions the first serious platform system built on top of stable account identity.

## Mental model

Ownership answers:
- who does this node belong to?

Permissions answer:
- what is this identity allowed to do?

Examples:
- `ui.mechanic.open`
- `ui.police.open`
- `business.manage`
- `ownership.manage`
- `permissions.manage`
- `permissions.manage.scope`

## Scope shape

Permissions may be:
- global: `scope_type = NULL`, `scope_key = NULL`
- scoped: for example `scope_type = node`, `scope_key = business:rusty_wrench_shop`

## Owner-implied access

For node-scoped access, owners are treated as implicitly allowed for a limited set of permissions:
- `ownership.use`
- `ownership.manage`
- `ownership.release`
- `ownership.transfer`
- `permissions.manage.scope`
- `ui.node.manage`

That means a node owner can manage access to their own node without needing a fully global admin role.

## Important current behavior

This patch keeps the system generic on purpose.
It does not hardcode job concepts like mechanic or police into the backend.
Instead, jobs should map to permission keys.

Examples:
- mechanic -> `ui.mechanic.open`, `vehicle.service.perform`
- police -> `ui.police.open`, `records.view`
- node owner -> owner-implied node-scoped management

## Suggested first tests

1. Claim a node with your current account
2. Check `pz.bridge.ownership.isOwner`
3. Grant a node-scoped permission to another account
4. Check `pz.bridge.permissions.has`
5. Call `pz.bridge.permissions.explain` to verify the access source

## Recommended next step after this patch

Once this is working, update the README to reflect:
- Java-side identity detector + re-arm
- current bridge method surface
- ownership + permissions model
- suggested permission key naming conventions
