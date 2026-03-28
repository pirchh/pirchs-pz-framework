package pirch.pz.service;

/**
 * Resolves the authoritative local identity from IdentityLifecycleService.
 *
 * Banking self-methods should use the same lifecycle-promoted identity that the
 * rest of the bridge uses, rather than rebuilding a legacy identity from
 * IsoPlayer-visible ids.
 */
public final class LocalPlayerIdentityResolver {
    private LocalPlayerIdentityResolver() {
    }

    public static PlayerIdentity requireLocalIdentity() {
        if (!IdentityLifecycleService.isReady()) {
            throw new IllegalStateException("local player identity lifecycle is not ready");
        }

        if (!IdentityLifecycleService.hasResolvedLocalAccount()) {
            throw new IllegalStateException("local player account has not been resolved yet");
        }

        PlayerIdentity identity = IdentityLifecycleService.getLastIdentity();
        if (identity == null) {
            throw new IllegalStateException("local player identity is missing from lifecycle state");
        }

        return PlayerIdentityService.validate(identity);
    }

    public static Integer requireLocalAccountId() {
        if (!IdentityLifecycleService.isReady()) {
            throw new IllegalStateException("local player identity lifecycle is not ready");
        }

        Integer accountId = IdentityLifecycleService.getLastResolvedAccountId();
        if (accountId == null) {
            throw new IllegalStateException("local player account id has not been resolved yet");
        }

        return accountId;
    }

    public static boolean isResolved() {
        return IdentityLifecycleService.isReady()
            && IdentityLifecycleService.hasResolvedLocalAccount()
            && IdentityLifecycleService.getLastIdentity() != null;
    }

    public static String getResolutionSource() {
        return IdentityLifecycleService.getLastResolutionSource();
    }

    public static boolean isAuthoritative() {
        return IdentityLifecycleService.isLastResolutionAuthoritative();
    }
}