package pirch.pz.debug;

import java.util.List;
import pirch.pz.service.PlayerContextResolver;
import pirch.pzloader.util.LoaderLog;
import zombie.characters.IsoPlayer;

public final class IdentityDiscoveryWatcher {
    private static final long POLL_INTERVAL_MS = 1000L;
    private static final int MAX_ATTEMPTS = 180;

    private static volatile boolean started = false;
    private static volatile boolean resolved = false;

    private IdentityDiscoveryWatcher() {
    }

    public static synchronized void start() {
        if (started) {
            LoaderLog.info("[PZLIFE][IDENTITY] Identity discovery watcher already started.");
            return;
        }

        started = true;
        LoaderLog.info("[PZLIFE][IDENTITY] Starting Java-side identity discovery watcher. intervalMs="
                + POLL_INTERVAL_MS + ", maxAttempts=" + MAX_ATTEMPTS);

        Thread thread = new Thread(() -> runWatcher(), "pirchs-pz-identity-discovery-watcher");
        thread.setDaemon(true);
        thread.start();
    }

    private static void runWatcher() {
        PlayerContextResolver resolver = new PlayerContextResolver();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            if (resolved) {
                LoaderLog.info("[PZLIFE][IDENTITY] Identity already resolved. Watcher exiting.");
                return;
            }

            try {
                int visiblePlayers = countVisiblePlayers();
                if (attempt == 1 || attempt % 5 == 0 || visiblePlayers > 0) {
                    LoaderLog.info("[PZLIFE][IDENTITY][watcher] attempt=" + attempt + "/" + MAX_ATTEMPTS
                            + ", visiblePlayers=" + visiblePlayers);
                }

                IsoPlayer player = resolver.tryResolveAnyPlayer();
                if (player != null) {
                    int playerNum = safePlayerNum(player);
                    LoaderLog.info("[PZLIFE][IDENTITY][watcher] IsoPlayer found on attempt " + attempt
                            + ". playerNum=" + playerNum);
                    IdentityLifecycleBridge.onLocalPlayerCreated(playerNum, player);
                    resolved = true;
                    return;
                }
            } catch (Exception e) {
                LoaderLog.info("[PZLIFE][IDENTITY][watcher] attempt=" + attempt + " not ready yet: " + e.getMessage());
            }

            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LoaderLog.error("[PZLIFE][IDENTITY] Identity discovery watcher interrupted: " + e.getMessage());
                return;
            }
        }

        LoaderLog.info("[PZLIFE][IDENTITY] Identity discovery watcher ended without resolving a player.");
    }

    private static int countVisiblePlayers() {
        List<IsoPlayer> players = IsoPlayer.getPlayers();
        if (players == null) {
            return 0;
        }

        int count = 0;
        for (IsoPlayer player : players) {
            if (player != null) {
                count++;
            }
        }
        return count;
    }

    private static int safePlayerNum(IsoPlayer player) {
        try {
            return player.getPlayerNum();
        } catch (Exception ignored) {
            return 0;
        }
    }
}
