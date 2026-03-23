# Role Service + Bootstrap Fix

The previous seeding-only patch replaced `RoleService` with a stripped-down version.
That fixed the bootstrap duplicate insert, but it removed the methods that `RoleBridge`
still calls:

- assign(...)
- revoke(...)
- has(...)
- list(...)

That is why your build failed in `RoleBridge.java`.

This patch restores the full scoped-role service/repository surface and keeps the
bootstrap seeding fix in place.

## Apply
Overwrite these two files into your repo, then run:

```powershell
gradlew clean build
```

If that succeeds, then run your normal install flow.
