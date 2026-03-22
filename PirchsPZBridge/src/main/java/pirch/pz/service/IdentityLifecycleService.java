package pirch.pz.service;

import pirch.pzloader.util.LoaderLog;
import zombie.characters.IsoPlayer;

public final class IdentityLifecycleService {
    private static volatile boolean ready = false;
    private static volatile String lastResolvedAccountExternalId = null;
    private static volatile Integer lastResolvedAccountId = null;
    private static volatile PlayerIdentity lastIdentity = null;
    private static volatile Integer lastResolvedPlayerNum = null;

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
            );
        }
        lastResolvedAccountExternalId = null;
        lastResolvedAccountId = null;
        lastIdentity = null;
        lastResolvedPlayerNum = null;
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

    public static IdentityLifecycleState snapshot() {
        return new IdentityLifecycleState(ready, lastIdentity != null, lastResolvedAccountId, lastIdentity);
    }

    public static synchronized PlayerIdentity resolveAndPromoteLocalPlayer(int playerNum, IsoPlayer player) {
        if (!ready) {
            LoaderLog.info("[PZLIFE][IDENTITY] lifecycle received player before ready state.");
            return null;
        }
        if (player == null) {
            LoaderLog.info("[PZLIFE][IDENTITY] lifecycle received null player.");
            return null;
        }

        PlayerIdentity identity = new PzPlayerIdentityAdapter().fromIsoPlayer(player);
        String accountExternalId = identity.getAccountExternalId();
        if (accountExternalId == null || accountExternalId.isBlank()) {
            LoaderLog.info("[PZLIFE][IDENTITY] lifecycle resolved a player but no accountExternalId was available yet.");
            return null;
        }

        if (accountExternalId.equals(lastResolvedAccountExternalId) && lastResolvedAccountId != null) {
            if (PzRuntimeConfig.isVerboseIdentityLoggingEnabled()) {
                LoaderLog.info("[PZLIFE][IDENTITY] duplicate lifecycle resolution ignored for " + accountExternalId);
            }
            lastIdentity = identity;
            lastResolvedPlayerNum = playerNum;
            return identity;
        }

        int accountId = AccountService.resolveOrCreateAccount(identity);
        lastResolvedAccountExternalId = accountExternalId;
        lastResolvedAccountId = accountId;
        lastIdentity = identity;
        lastResolvedPlayerNum = playerNum;

        LoaderLog.info(
            "[PZLIFE][IDENTITY] resolved account from lifecycle. playerNum=" + playerNum
                + ", accountId=" + accountId
                + ", accountExternalId=" + accountExternalId
        );
        return identity;
    }
}
