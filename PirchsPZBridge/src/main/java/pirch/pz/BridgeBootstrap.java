package pirch.pz;

import pirch.pz.bridge.DebugBridge;
import zombie.debug.DebugLog;

/**
 * Bridge bootstrap should only initialize Java-side services and register bridge methods.
 *
 * <p>Lua exposure is now expected to happen from the patched engine LuaManager.Exposer.exposeAll()
 * </p>
 */
public final class BridgeBootstrap {
    private static volatile boolean initialized;

    private BridgeBootstrap() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            DebugLog.General.debugln("[PZLIFE][BOOT] BridgeBootstrap already initialized");
            return;
        }

        // Keep your existing service/bootstrap chain here.
        // The key point of this refactor is that Java registers methods here,
        // while Lua exposure happens later from patched LuaManager.Exposer.
        DebugBridge.register();

        initialized = true;
        DebugLog.General.debugln("[PZLIFE][BOOT] BridgeBootstrap.initialize() completed");
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
