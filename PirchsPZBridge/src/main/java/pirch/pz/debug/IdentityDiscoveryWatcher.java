package pirch.pz.debug;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import pirch.pz.service.IdentityLifecycleService;
import pirch.pz.service.PlayerContextResolver;
import pirch.pz.service.PzRuntimeConfig;
import pirch.pzloader.util.LoaderLog;
import zombie.characters.IsoPlayer;

public final class IdentityDiscoveryWatcher {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final AtomicInteger ATTEMPT_COUNTER = new AtomicInteger(0);
    private static volatile ScheduledExecutorService scheduler;

    private IdentityDiscoveryWatcher() {
    }

    public static synchronized void start() {
        if (!PzRuntimeConfig.isIdentityWatcherEnabled()) {
            LoaderLog.info("[PZLIFE][IDENTITY] Java-side local-player detector disabled by config.");
            return;
        }

        if (STARTED.get()) {
            if (PzRuntimeConfig.isVerboseIdentityLoggingEnabled()) {
                LoaderLog.info("[PZLIFE][IDENTITY] Java-side local-player detector already running.");
            }
            return;
        }

        STARTED.set(true);
        ATTEMPT_COUNTER.set(0);
        scheduler = Executors.newSingleThreadScheduledExecutor(new DetectorThreadFactory());

        long pollIntervalMs = PzRuntimeConfig.getIdentityWatcherPollMs();
        int maxAttempts = PzRuntimeConfig.getIdentityWatcherMaxAttempts();
        String maxAttemptsText = maxAttempts > 0 ? String.valueOf(maxAttempts) : "unlimited";

        LoaderLog.info("[PZLIFE][IDENTITY] Starting Java-side local-player detector. intervalMs="
            + pollIntervalMs + ", maxAttempts=" + maxAttemptsText);

        scheduler.scheduleWithFixedDelay(
            IdentityDiscoveryWatcher::tick,
            0L,
            pollIntervalMs,
            TimeUnit.MILLISECONDS
        );
    }

    public static synchronized void stop() {
        ScheduledExecutorService current = scheduler;
        scheduler = null;
        STARTED.set(false);
        ATTEMPT_COUNTER.set(0);

        if (current != null) {
            current.shutdownNow();
        }
    }

    public static synchronized void rearm() {
        stop();
        IdentityLifecycleService.resetLocalResolution();
        start();
    }

    private static void tick() {
        if (!STARTED.get()) {
            return;
        }

        if (!IdentityLifecycleService.isReady()) {
            if (PzRuntimeConfig.isVerboseIdentityLoggingEnabled()) {
                LoaderLog.info("[PZLIFE][IDENTITY][detector] lifecycle not ready yet; skipping check.");
            }
            return;
        }

        if (IdentityLifecycleService.hasResolvedLocalAccount()) {
            String accountExternalId = IdentityLifecycleService.getLastResolvedAccountExternalId();
            LoaderLog.info("[PZLIFE][IDENTITY] Local-player detector resolved account and is stopping. accountExternalId="
                + accountExternalId);
            stop();
            return;
        }

        int attempt = ATTEMPT_COUNTER.incrementAndGet();
        int maxAttempts = PzRuntimeConfig.getIdentityWatcherMaxAttempts();
        if (maxAttempts > 0 && attempt > maxAttempts) {
            LoaderLog.info("[PZLIFE][IDENTITY] Java-side local-player detector reached maxAttempts=" + maxAttempts
                + " without resolving a player. Stopping detector.");
            stop();
            return;
        }

        try {
            PlayerContextResolver resolver = new PlayerContextResolver();
            int visiblePlayers = countVisiblePlayers();
            int logEveryAttempts = Math.max(1, PzRuntimeConfig.getIdentityWatcherLogEveryAttempts());
            boolean shouldLogAttempt = attempt == 1 || visiblePlayers > 0 || attempt % logEveryAttempts == 0;

            if (shouldLogAttempt) {
                LoaderLog.info("[PZLIFE][IDENTITY][detector] attempt=" + attempt
                    + formatMaxAttempts(maxAttempts) + ", visiblePlayers=" + visiblePlayers);
            }

            IsoPlayer player = resolver.tryResolveAnyPlayer();
            if (player == null) {
                return;
            }

            int playerNum = safePlayerNum(player);
            LoaderLog.info("[PZLIFE][IDENTITY][detector] IsoPlayer found on attempt " + attempt
                + ". playerNum=" + playerNum);
            IdentityLifecycleBridge.onLocalPlayerCreated(playerNum, player);

            if (IdentityLifecycleService.hasResolvedLocalAccount()) {
                stop();
            }
        } catch (Throwable t) {
            LoaderLog.error("[PZLIFE][IDENTITY][detector] local-player check failed: " + t.getMessage());
            if (PzRuntimeConfig.isVerboseIdentityLoggingEnabled()) {
                t.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static int countVisiblePlayers() {
        try {
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
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int safePlayerNum(IsoPlayer player) {
        try {
            return player.getPlayerNum();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String formatMaxAttempts(int maxAttempts) {
        return maxAttempts > 0 ? "/" + maxAttempts : "/unlimited";
    }

    private static final class DetectorThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "pirchs-pz-local-player-detector");
            thread.setDaemon(true);
            return thread;
        }
    }
}
