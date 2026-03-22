# Bridge API

This file documents the current public bridge methods after the identity, ownership, and permissions foundation pass.

## System

### `pz.bridge.system.ping`
Returns `pong`.

### `pz.bridge.system.version`
Returns bridge version text.

### `pz.bridge.system.healthCheck`
Returns a map including version, registered method count, lifecycle state, and whether database-backed modules are present.

### `pz.bridge.system.listMethods`
Returns all registered methods and metadata.

### `pz.bridge.system.dbPing`
Returns `db-ok` when the PostgreSQL connection succeeds.

### `pz.bridge.system.resolveAccount`
Input:
- structured `PlayerIdentity` map, or
- legacy string external id, or
- `(legacyExternalId, displayName)` pair

Returns:
- account id
- normalized identity
- canonical account external id
- canonical character external id

### `pz.bridge.player.resolveIdentity`
Input: structured map, `PlayerIdentity`, or legacy string.

Returns normalized identity map.

### `pz.bridge.player.getLifecycleState`
Returns current lifecycle state plus any last resolved identity.

### `pz.bridge.player.markLocalPlayerCreated`
Debug/helper entry point for explicitly promoting a resolved local player through the lifecycle pipeline.

## Bank

### `pz.bridge.bank.getBalance`
Input: identity
Returns wallet balance.

### `pz.bridge.bank.deposit`
Input: identity, integer amount
Returns new wallet balance.

### `pz.bridge.bank.withdraw`
Input: identity, integer amount
Returns new wallet balance.

## Ownership

### `pz.bridge.ownership.claimNode`
Input: identity, nodeKey, optional nodeType
Claims a node for the account if unowned.

### `pz.bridge.ownership.releaseNode`
Input: identity, nodeKey
Releases the node if the caller owns it.

### `pz.bridge.ownership.listOwnedNodes`
Input: identity
Returns all active node claims for the account.

### `pz.bridge.ownership.getNodeOwner`
Input: nodeKey
Returns the owner account for that node, if any.

## Permissions

### `pz.bridge.permissions.grant`
Input: actor identity, target identity, permissionKey, optional scopeType, optional scopeKey
Requires the actor to already have `permissions.manage`, unless the actor and target are the same account and the permission being granted is `ownership.use`.

### `pz.bridge.permissions.revoke`
Input: actor identity, target identity, permissionKey, optional scopeType, optional scopeKey
Same authorization rule as `grant`.

### `pz.bridge.permissions.has`
Input: target identity, permissionKey, optional scopeType, optional scopeKey
Returns `true` or `false`.

### `pz.bridge.permissions.list`
Input: target identity
Returns current active grants.
