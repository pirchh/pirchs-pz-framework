package pirch.pz.debug;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import pirch.pz.service.IdentityLifecycleService;
import pirch.pz.service.PlayerContextResolver;
import pirch.pz.service.PzRuntimeConfig;
import pirch.pzloader.util.LoaderLog;
import zombie.characters.IsoPlayer;

public final class IdentityDiscoveryWatcher {
    private static volatile boolean started = false;
    private static volatile ScheduledExecutorService executor;
    private static volatile int attemptCounter = 0;
    private static volatile int emptyChecksAfterResolve = 0;

    private IdentityDiscoveryWatcher() {
    }

    public static synchronized void start() {
        if (!PzRuntimeConfig.isIdentityDetectorEnabled()) {
            LoaderLog.info("[PZLIFE][IDENTITY] Java-side local-player detector is disabled by config.");
            return;
        }
        if (started && executor != null && !executor.isShutdown()) {
            LoaderLog.info("[PZLIFE][IDENTITY] Java-side local-player detector already armed.");
            return;
        }

        long pollMs = PzRuntimeConfig.getIdentityDetectorPollMs();
        int maxAttempts = PzRuntimeConfig.getIdentityDetectorMaxAttempts();
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "pirchs-pz-identity-detector");
            thread.setDaemon(true);
            return thread;
        });
        attemptCounter = 0;
        emptyChecksAfterResolve = 0;
        started = true;

        LoaderLog.info(
            "[PZLIFE][IDENTITY] Starting Java-side local-player detector. intervalMs=" + pollMs
                + ", maxAttempts=" + (maxAttempts <= 0 ? "unlimited" : maxAttempts)
                + ", authority=java"
        );

        executor.scheduleWithFixedDelay(IdentityDiscoveryWatcher::tick, 0L, pollMs, TimeUnit.MILLISECONDS);
    }

    public static synchronized void rearm(String reason) {
        IdentityLifecycleService.resetLocalResolution(reason);
        attemptCounter = 0;
        emptyChecksAfterResolve = 0;
        if (!started || executor == null || executor.isShutdown()) {
            LoaderLog.info("[PZLIFE][IDENTITY] re-arming Java-side local-player detector. reason=" + reason);
            start();
            return;
        }
        LoaderLog.info("[PZLIFE][IDENTITY] detector remains armed for next session. reason=" + reason);
    }

    private static void tick() {
        if (!IdentityLifecycleService.isReady()) {
            return;
        }

        PlayerContextResolver resolver = new PlayerContextResolver();
        IsoPlayer player = null;
        int visiblePlayers = 0;
        try {
            visiblePlayers = countVisiblePlayers();
            player = resolver.tryResolveAnyPlayer();
        } catch (Exception e) {
            LoaderLog.info("[PZLIFE][IDENTITY][detector] tick failed before readiness: " + e.getMessage());
            return;
        }

        if (IdentityLifecycleService.hasResolvedLocalAccount()) {
            monitorResolvedSession(player, visiblePlayers);
            if (IdentityLifecycleService.isLastResolutionAuthoritative()) {
                return;
            }
        }

        attemptCounter++;
        int maxAttempts = PzRuntimeConfig.getIdentityDetectorMaxAttempts();
        if (shouldLogAttempt(attemptCounter, visiblePlayers)) {
            LoaderLog.info(
                "[PZLIFE][IDENTITY][detector] attempt=" + attemptCounter + "/"
                    + (maxAttempts <= 0 ? "unlimited" : maxAttempts)
                    + ", visiblePlayers=" + visiblePlayers
                    + ", lastSource=" + IdentityLifecycleService.getLastResolutionSource()
                    + ", lastAuthoritative=" + IdentityLifecycleService.isLastResolutionAuthoritative()
            );
        }

        if (player == null) {
            if (maxAttempts > 0 && attemptCounter >= maxAttempts) {
                LoaderLog.info(
                    "[PZLIFE][IDENTITY][detector] reached maxAttempts without a local player. "
                        + "Continuing to stay armed for future session detection."
                );
                attemptCounter = 0;
            }
            return;
        }

        int playerNum = safePlayerNum(player);
        LoaderLog.info(
            "[PZLIFE][IDENTITY][detector] IsoPlayer found on attempt " + attemptCounter + ". playerNum=" + playerNum
        );
        IdentityLifecycleBridge.onLocalPlayerCreated(playerNum, player);
        emptyChecksAfterResolve = 0;
    }

    private static void monitorResolvedSession(IsoPlayer player, int visiblePlayers) {
        if (!PzRuntimeConfig.isIdentitySessionMonitoringEnabled()) {
            return;
        }

        if (player != null && visiblePlayers > 0) {
            emptyChecksAfterResolve = 0;
            return;
        }

        emptyChecksAfterResolve++;
        int threshold = Math.max(1, PzRuntimeConfig.getIdentitySessionEmptyChecksToRearm());
        if (PzRuntimeConfig.isVerboseIdentityLoggingEnabled()) {
            LoaderLog.info(
                "[PZLIFE][IDENTITY][detector] resolved session appears absent. emptyChecks="
                    + emptyChecksAfterResolve + "/" + threshold
            );
        }

        if (emptyChecksAfterResolve < threshold) {
            return;
        }

        Integer accountId = IdentityLifecycleService.getLastResolvedAccountId();
        String accountExternalId = IdentityLifecycleService.getLastResolvedAccountExternalId();
        LoaderLog.info(
            "[PZLIFE][IDENTITY][detector] local session ended; re-arming lifecycle detector. "
                + "lastAccountId=" + accountId + ", lastAccountExternalId=" + accountExternalId
        );
        rearm("resolved session disappeared from Java-side detector");
    }

    private static boolean shouldLogAttempt(int attempt, int visiblePlayers) {
        int logEvery = Math.max(1, PzRuntimeConfig.getIdentityDetectorLogEveryAttempts());
        return attempt == 1 || visiblePlayers > 0 || attempt % logEvery == 0;
    }

    private static int countVisiblePlayers() {
        List<IsoPlayer> players = IsoPlayer.getPlayers();
        if (players == null) {
            return 0;
        }
        int count = 0;
        for (IsoPlayer candidate : players) {
            if (candidate != null) {
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
