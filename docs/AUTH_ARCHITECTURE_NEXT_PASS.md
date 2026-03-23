# Auth Architecture Next Pass

This overwrite pack is a best-effort next pass for:

- bootstrap admin seeding
- roles as permission bundles
- owner-scoped permission delegation
- self-test setup using fixture-style actors instead of weakening production rules

## Intent

1. Keep production auth strict.
2. Seed authority intentionally.
3. Let tests use seeded/admin-capable actors.
4. Let owners delegate scoped permissions on nodes they own.
5. Keep roles as a bundle layer over permissions rather than hardcoded game concepts.
