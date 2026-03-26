package pirch.pzloader.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;
import pirch.pzloader.util.LoaderLog;

public final class LoaderBootstrap {
    private static final Object LOCK = new Object();

    private static volatile boolean initialized = false;
    private static volatile long initializedAtEpochMs = 0L;
    private static volatile String initializedThread = "";

    private LoaderBootstrap() {
    }

    public static void initialize() {
        synchronized (LOCK) {
            if (initialized) {
                LoaderLog.info("Loader already initialized.");
                return;
            }

            LoaderLog.info("Initializing loader...");
            initialized = true;
            initializedAtEpochMs = System.currentTimeMillis();
            initializedThread = Thread.currentThread().getName();
            LoaderLog.info("Loader initialization complete.");
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static Map<String, Object> snapshot() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("initialized", initialized);
        state.put("initializedAtEpochMs", initializedAtEpochMs);
        state.put("initializedThread", initializedThread);
        return state;
    }
}
