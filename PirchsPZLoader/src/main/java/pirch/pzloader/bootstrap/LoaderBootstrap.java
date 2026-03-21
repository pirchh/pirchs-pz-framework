package pirch.pzloader.bootstrap;

import pirch.pzloader.util.LoaderLog;

public final class LoaderBootstrap {
    private static boolean initialized = false;

    private LoaderBootstrap() {
    }

    public static void initialize() {
        if (initialized) {
            LoaderLog.info("Loader already initialized.");
            return;
        }

        LoaderLog.info("Initializing loader...");
        initialized = true;
        LoaderLog.info("Loader initialization complete.");
    }

    public static boolean isInitialized() {
        return initialized;
    }
}