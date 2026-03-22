package pirch.pz;

import java.util.LinkedHashMap;
import java.util.Map;
import pirch.pz.bridge.BankBridge;
import pirch.pz.bridge.SystemBridge;
import pirch.pz.db.SchemaManager;
import pirch.pz.debug.IdentityDiscoveryWatcher;
import pirch.pz.debug.IdentityLifecycleBridge;
import pirch.pzloader.bootstrap.LoaderBootstrap;
import pirch.pzloader.runtime.ModuleRegistry;
import pirch.pzloader.util.LoaderLog;

public final class BridgeBootstrap {
    private static boolean initialized = false;

    private BridgeBootstrap() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            LoaderLog.info("PirchsPZBridge already initialized.");
            return;
        }

        try {
            LoaderBootstrap.initialize();
            SchemaManager.initialize();

            SystemBridge.register();
            BankBridge.register();

            initialized = true;

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("registeredMethodCount", ModuleRegistry.count());
            summary.put("registeredMethods", ModuleRegistry.getAllDefinitions().keySet());

            LoaderLog.info("PirchsPZBridge initialization complete: " + summary);
            LoaderLog.info("[PZLIFE][IDENTITY] Java-side identity discovery armed.");
            LoaderLog.info("[PZLIFE][IDENTITY] Goal: resolve a stable account/character identity from IsoPlayer and stop.");
            IdentityLifecycleBridge.markReady();
            IdentityDiscoveryWatcher.start();
        } catch (Exception e) {
            LoaderLog.error("PirchsPZBridge initialization failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("PirchsPZBridge failed to initialize.", e);
        }
    }
}
