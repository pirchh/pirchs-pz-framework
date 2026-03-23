This patch aligns the current bridge layer with the service/repository split.

What changed:
- OwnershipService now wraps low-level repository calls using PlayerIdentity.
- PermissionService now wraps low-level repository calls using PlayerIdentity.
- AuthSelfTestService now exposes:
  - reset(String)
  - onLocalAccountResolved(PlayerIdentity, int)
  - runAfterLocalResolution(PlayerIdentity, int)
  - getLastStatus()

Why:
The build errors showed a bridge -> service API mismatch and a service -> repository API mismatch. The bridges were calling high-level PlayerIdentity methods, while the service/repo layers still only exposed lower-level Connection/accountId methods.