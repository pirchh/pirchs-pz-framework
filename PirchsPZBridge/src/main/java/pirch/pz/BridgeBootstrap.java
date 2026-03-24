package pirch.pz;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import pirch.pz.bridge.BankBridge;
import pirch.pz.bridge.DebugBridge;
import pirch.pz.bridge.OwnershipBridge;
import pirch.pz.bridge.PermissionBridge;
import pirch.pz.bridge.RoleBridge;
import pirch.pz.bridge.SystemBridge;
import pirch.pz.db.DatabaseManager;
import pirch.pz.db.SchemaManager;
import pirch.pz.debug.IdentityDiscoveryWatcher;
import pirch.pz.debug.IdentityLifecycleBridge;
import pirch.pz.service.AuthSelfTestService;
import pirch.pz.service.BootstrapAdminService;
import pirch.pz.service.PlayerIdentity;
import pirch.pz.service.PzRuntimeConfig;
import pirch.pz.service.RoleService;
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
            seedAuthBootstrap();

            SystemBridge.register();
            BankBridge.register();
            OwnershipBridge.register();
            PermissionBridge.register();
            RoleBridge.register();
            if (PzRuntimeConfig.isDebugBridgeEnabled()) {
                DebugBridge.register();
            }

            initialized = true;

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("registeredMethodCount", ModuleRegistry.count());
            summary.put("registeredMethods", ModuleRegistry.getAllDefinitions().keySet());
            summary.put("debugBridgeEnabled", PzRuntimeConfig.isDebugBridgeEnabled());

            LoaderLog.info("PirchsPZBridge initialization complete: " + summary);
            LoaderLog.info("[PZLIFE][IDENTITY] Java-side identity detector armed.");
            LoaderLog.info("[PZLIFE][IDENTITY] Strategy: single lifecycle promotion path, watcher stays as fallback.");
            LoaderLog.info("[PZLIFE][AUTH] Single-owner nodes with delegated scoped access enabled.");
            LoaderLog.info("[PZLIFE][AUTH] Roles enabled as permission bundles for global and node-scoped access.");
            LoaderLog.info("[PZLIFE][AUTH][selftest] Java-side self-test enabled=" + PzRuntimeConfig.isAuthSelfTestEnabled());
            LoaderLog.info("[PZLIFE][AUTH][debug] Debug bridge enabled=" + PzRuntimeConfig.isDebugBridgeEnabled());

            IdentityLifecycleBridge.markReady();
            IdentityDiscoveryWatcher.start();
        } catch (Exception e) {
            LoaderLog.error("PirchsPZBridge initialization failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("PirchsPZBridge failed to initialize.", e);
        }
    }

    public static void note() {
        LoaderLog.info("[PZLIFE][AUTH] next-pass overwrite pack includes bootstrap-admin, scoped roles, owner-scoped delegation, and debug test helpers.");
    }

    public static void onLocalAccountResolvedForSelfTest(PlayerIdentity identity, int accountId) {
        AuthSelfTestService.runAfterLocalResolution(identity, accountId);
    }

    private static void seedAuthBootstrap() throws Exception {
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            RoleService.ensureCoreRoles(connection);

            if (PzRuntimeConfig.isBootstrapAdminEnabled()) {
                int bootstrapAdminId = BootstrapAdminService.ensureBootstrapAdmin(connection);
                LoaderLog.info("[PZLIFE][AUTH] bootstrap admin ensured. accountId=" + bootstrapAdminId);
            }

            connection.commit();
        }
    }
}
