# Auth + Node Stabilization Next Pass

This overwrite pack does four things:

1. keeps nodes as single-owner only
2. makes delegated access explicit through scoped permissions and scoped roles
3. keeps owner-implied access narrow and intentional
4. reduces duplicate lifecycle behavior by treating the watcher as a fallback rather than a second auth system

## Core model

- One owner account per node.
- Other accounts can receive scoped permissions on that node.
- Roles are permission bundles.
- Admin can assign global roles.
- Node owners can assign node-scoped roles and node-scoped permissions for nodes they own.

## Important behavior changes

### Ownership
`claimNode` no longer silently steals an active node from another account.
If another account already owns the node, the call returns `claimed=false` with `reason=already_owned`.

### Permissions
Permissions now resolve from:
- direct scoped grant
- direct global grant
- role grant
- owner-implied access

### Roles
Roles now support:
- global assignment
- node-scoped assignment

Examples:
- `business_manager` scoped to `node/business:rusty-wrench`
- `keyholder` scoped to `node/vehicle:sedan-001`

### Lifecycle
The watcher still exists, but routes into the same lifecycle promotion path.
The intent is one identity promotion pipeline, not multiple competing ones.

## Suggested first gameplay proof

Use vehicles first:

- owner claims a vehicle node
- owner grants `keyholder` role scoped to that vehicle node
- target account can pass `vehicle.use` / `vehicle.drive`
- target account still does not become the owner
