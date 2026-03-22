package pirch.pz.debug;

import pirch.pz.service.PlayerContextResolver;
import pirch.pz.service.PlayerIdentity;
import pirch.pzloader.util.LoaderLog;

public final class IdentityTestHook {
    private static final long POLL_INTERVAL_MS = 2000L;
    private static final int MAX_ATTEMPTS = 60;

    private static volatile boolean started = false;
    private static volatile boolean resolved = false;

    private IdentityTestHook() {
    }

    public static synchronized void startIdentityPolling() {
        if (started) {
            LoaderLog.info("[PZLIFE][IDENTITY] Identity polling already started.");
            return;
        }

        started = true;

        LoaderLog.info("[PZLIFE][IDENTITY] Starting identity polling. intervalMs="
                + POLL_INTERVAL_MS + ", maxAttempts=" + MAX_ATTEMPTS);

        Thread thread = new Thread(() -> {
            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                if (resolved) {
                    LoaderLog.info("[PZLIFE][IDENTITY] Identity already resolved. Poller exiting.");
                    return;
                }

                try {
                    PlayerIdentity identity = new PlayerContextResolver().resolveLocalIdentity();
                    logResolvedIdentity(identity, attempt);
                    resolved = true;
                    return;
                } catch (Exception e) {
                    LoaderLog.info("[PZLIFE][IDENTITY][attempt=" + attempt + "/" + MAX_ATTEMPTS
                            + "] no player resolved yet: " + e.getMessage());
                }

                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LoaderLog.error("[PZLIFE][IDENTITY] Identity polling interrupted: " + e.getMessage());
                    return;
                }
            }

            LoaderLog.info("[PZLIFE][IDENTITY] Identity polling ended without resolving a player.");
        }, "pirchs-pz-identity-poller");

        thread.setDaemon(true);
        thread.start();
    }

    private static void logResolvedIdentity(PlayerIdentity identity, int attempt) {
        LoaderLog.info("[PZLIFE][IDENTITY] Identity resolved on attempt " + attempt + ".");
        LoaderLog.info("[PZLIFE][IDENTITY] playerSource=" + identity.getPlayerSource());
        LoaderLog.info("[PZLIFE][IDENTITY] sourcePlayerId=" + identity.getSourcePlayerId());
        LoaderLog.info("[PZLIFE][IDENTITY] steamId=" + identity.getSteamId());
        LoaderLog.info("[PZLIFE][IDENTITY] username=" + identity.getUsername());
        LoaderLog.info("[PZLIFE][IDENTITY] displayName=" + identity.getDisplayName());
        LoaderLog.info("[PZLIFE][IDENTITY] canonicalExternalId=" + identity.getCanonicalExternalId());
    }
}
