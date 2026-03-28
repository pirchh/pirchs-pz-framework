package pirch.pz.service;

import pirch.pzloader.util.LoaderLog;
import zombie.characters.IsoPlayer;

public final class IdentityLifecycleService {
    private static volatile boolean ready = false;
    private static volatile String lastResolvedAccountExternalId = null;
    private static volatile Integer lastResolvedAccountId = null;
    private static volatile PlayerIdentity lastIdentity = null;
    private static volatile Integer lastResolvedPlayerNum = null;
    private static volatile String lastResolutionSource = "unresolved";
    private static volatile boolean lastResolutionAuthoritative = false;
    private static volatile long lastResolutionEpochMs = 0L;

    private IdentityLifecycleService() {
    }

    public static synchronized void markReady() {
        ready = true;
    }

    public static synchronized void markNotReady() {
        ready = false;
    }

    public static synchronized void resetLocalResolution() {
        resetLocalResolution("manual reset");
    }

    public static synchronized void resetLocalResolution(String reason) {
        if (PzRuntimeConfig.isVerboseIdentityLoggingEnabled() && hasResolvedLocalAccount()) {
            LoaderLog.info(
                "[PZLIFE][IDENTITY] resetting local lifecycle state. reason=" + reason
                    + ", lastAccountId=" + lastResolvedAccountId
                    + ", lastAccountExternalId=" + lastResolvedAccountExternalId
                    + ", lastResolutionSource=" + lastResolutionSource
                    + ", authoritative=" + lastResolutionAuthoritative
            );
        }

        lastResolvedAccountExternalId = null;
        lastResolvedAccountId = null;
        lastIdentity = null;
        lastResolvedPlayerNum = null;
        lastResolutionSource = "unresolved";
        lastResolutionAuthoritative = false;
        lastResolutionEpochMs = 0L;

        AuthSelfTestService.reset(reason);
    }

    public static synchronized void resetAll() {
        resetAll("manual full reset");
    }

    public static synchronized void resetAll(String reason) {
        markNotReady();
        resetLocalResolution(reason);
    }

    public static boolean isReady() {
        return ready;
    }

    public static boolean hasResolvedLocalAccount() {
        return lastResolvedAccountId != null && lastResolvedAccountExternalId != null;
    }

    public static String getLastResolvedAccountExternalId() {
        return lastResolvedAccountExternalId;
    }

    public static Integer getLastResolvedAccountId() {
        return lastResolvedAccountId;
    }

    public static Integer getLastResolvedPlayerNum() {
        return lastResolvedPlayerNum;
    }

    public static PlayerIdentity getLastIdentity() {
        return lastIdentity;
    }

    public static String getLastResolutionSource() {
        return lastResolutionSource;
    }

    public static boolean isLastResolutionAuthoritative() {
        return lastResolutionAuthoritative;
    }

    public static long getLastResolutionEpochMs() {
        return lastResolutionEpochMs;
    }

    public static IdentityLifecycleState snapshot() {
        return new IdentityLifecycleState(
            ready,
            lastIdentity != null,
            lastResolvedAccountId,
            lastIdentity,
            lastResolutionSource,
            lastResolutionAuthoritative
        );
    }

    public static synchronized PlayerIdentity resolveAndPromoteLocalPlayer(int playerNum, IsoPlayer player) {
        return resolveAndPromoteLocalPlayer(playerNum, player, "legacy-unspecified", false);
    }

    public static synchronized PlayerIdentity resolveAndPromoteLocalPlayer(
        int playerNum,
        IsoPlayer player,
        String source,
        boolean authoritative
    ) {
        String safeSource = source == null || source.isBlank() ? "unspecified" : source;

        if (!ready) {
            LoaderLog.info("[PZLIFE][IDENTITY] lifecycle received player before ready state. source=" + safeSource);
            return null;
        }

        if (player == null) {
            LoaderLog.info("[PZLIFE][IDENTITY] lifecycle received null player. source=" + safeSource);
            return null;
        }

        PlayerIdentity identity = new PzPlayerIdentityAdapter().fromIsoPlayer(player);
        String accountExternalId = identity.getAccountExternalId();

        if (accountExternalId == null || accountExternalId.isBlank()) {
            LoaderLog.info(
                "[PZLIFE][IDENTITY] lifecycle resolved a player but no accountExternalId was available yet. source="
                    + safeSource
            );
            return null;
        }

        if (hasResolvedLocalAccount() && lastResolutionAuthoritative && !authoritative) {
            if (PzRuntimeConfig.isVerboseIdentityLoggingEnabled()) {
                LoaderLog.info(
                    "[PZLIFE][IDENTITY] non-authoritative resolution ignored because Java-authoritative identity already exists. "
                        + "source=" + safeSource
                        + ", accountExternalId=" + accountExternalId
                        + ", lockedSource=" + lastResolutionSource
                        + ", lockedAccountExternalId=" + lastResolvedAccountExternalId
                );
            }
            return lastIdentity;
        }

        if (accountExternalId.equals(lastResolvedAccountExternalId) && lastResolvedAccountId != null) {
            lastIdentity = identity;
            lastResolvedPlayerNum = playerNum;

            if (authoritative && !lastResolutionAuthoritative) {
                lastResolutionAuthoritative = true;
                lastResolutionSource = safeSource;
                lastResolutionEpochMs = System.currentTimeMillis();
                LoaderLog.info(
                    "[PZLIFE][IDENTITY] authoritative Java lifecycle path replaced prior non-authoritative resolution. "
                        + "accountExternalId=" + accountExternalId
                        + ", source=" + safeSource
                );
            } else if (PzRuntimeConfig.isVerboseIdentityLoggingEnabled()) {
                LoaderLog.info(
                    "[PZLIFE][IDENTITY] duplicate lifecycle resolution ignored for " + accountExternalId
                        + ". source=" + safeSource
                        + ", authoritative=" + authoritative
                );
            }

            return identity;
        }

        int accountId = AccountService.resolveOrCreateAccount(identity);

        lastResolvedAccountExternalId = accountExternalId;
        lastResolvedAccountId = accountId;
        lastIdentity = identity;
        lastResolvedPlayerNum = playerNum;
        lastResolutionSource = safeSource;
        lastResolutionAuthoritative = authoritative;
        lastResolutionEpochMs = System.currentTimeMillis();

        LoaderLog.info(
            "[PZLIFE][IDENTITY] resolved account from lifecycle. playerNum=" + playerNum
                + ", accountId=" + accountId
                + ", accountExternalId=" + accountExternalId
                + ", source=" + safeSource
                + ", authoritative=" + authoritative
        );

        AuthSelfTestService.onLocalAccountResolved(identity, accountId);
        AuthSelfTestService.runAfterLocalResolution(identity, accountId);

        return identity;
    }
}
