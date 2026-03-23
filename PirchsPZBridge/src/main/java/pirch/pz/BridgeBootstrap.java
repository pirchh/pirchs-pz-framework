package pirch.pz;

import java.util.LinkedHashMap;
import java.util.Map;
import pirch.pz.bridge.BankBridge;
import pirch.pz.bridge.OwnershipBridge;
import pirch.pz.bridge.PermissionBridge;
import pirch.pz.bridge.SystemBridge;
import pirch.pz.db.SchemaManager;
import pirch.pz.debug.IdentityDiscoveryWatcher;
import pirch.pz.debug.IdentityLifecycleBridge;
import pirch.pz.service.AuthSelfTestService;
import pirch.pz.service.PlayerIdentity;
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
            OwnershipBridge.register();
            PermissionBridge.register();

            initialized = true;

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("registeredMethodCount", ModuleRegistry.count());
            summary.put("registeredMethods", ModuleRegistry.getAllDefinitions().keySet());

            LoaderLog.info("PirchsPZBridge initialization complete: " + summary);
            LoaderLog.info("[PZLIFE][IDENTITY] Java-side identity detector armed.");
            LoaderLog.info("[PZLIFE][IDENTITY] Goal: resolve a stable account identity first, character identity second.");
            LoaderLog.info("[PZLIFE][IDENTITY] Strategy: low-frequency Java detection that remains armed across session resets.");
            LoaderLog.info("[PZLIFE][AUTH] Ownership + permissions v1 enabled.");
            LoaderLog.info("[PZLIFE][AUTH] Intent: generic authorization layer for menus, nodes, businesses, and world actions.");
            LoaderLog.info("[PZLIFE][AUTH][selftest] Java-side self-test enabled. It will run once after local account resolution each session.");

            IdentityLifecycleBridge.markReady();
            IdentityDiscoveryWatcher.start();
        } catch (Exception e) {
            LoaderLog.error("PirchsPZBridge initialization failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("PirchsPZBridge failed to initialize.", e);
        }
    }

    public static void note() {
        LoaderLog.info("[PZLIFE][AUTH] next-pass overwrite pack includes bootstrap-admin, roles, and owner-scoped delegation.");
    }

    public static void onLocalAccountResolvedForSelfTest(PlayerIdentity identity, int accountId) {
        AuthSelfTestService.runAfterLocalResolution(identity, accountId);
    }
}