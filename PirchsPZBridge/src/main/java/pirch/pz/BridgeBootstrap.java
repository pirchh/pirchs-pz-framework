package pirch.pz;

import java.util.Arrays;
import java.util.List;

import pirch.pz.bridge.BankBridge;
import pirch.pz.bridge.DebugBridge;
import pirch.pz.debug.IdentityDiscoveryWatcher;
import pirch.pz.debug.IdentityLifecycleBridge;
import pirch.pzloader.bootstrap.LoaderBootstrap;
import pirch.pzloader.runtime.ModuleRegistry;

import zombie.debug.DebugLog;

/**
 * Bridge bootstrap should only initialize Java-side services and register bridge methods.
 *
 * <p>Lua exposure is expected to happen from the patched engine LuaManager.Exposer.exposeAll().</p>
 */
public final class BridgeBootstrap {

    private static final List<String> REQUIRED_DEBUG_METHODS = Arrays.asList(
        "pz.bridge.debug.isLocalIdentityReady",
        "pz.bridge.debug.selfTestNow",
        "pz.bridge.debug.selfTestStatus",
        "pz.bridge.debug.runSmokeSuite",
        "pz.bridge.debug.resetLifecycle",
        "pz.bridge.debug.claimNode",
        "pz.bridge.debug.releaseNode",
        "pz.bridge.debug.getNodeOwner",
        "pz.bridge.debug.listOwnedNodes",
        "pz.bridge.debug.grantPermissionToSelf",
        "pz.bridge.debug.revokePermissionFromSelf",
        "pz.bridge.debug.hasPermission",
        "pz.bridge.debug.explainPermission",
        "pz.bridge.debug.listPermissions",
        "pz.bridge.debug.assignRoleToSelf",
        "pz.bridge.debug.revokeRoleFromSelf",
        "pz.bridge.debug.hasRole",
        "pz.bridge.debug.listRoles",
        "pz.bridge.debug.localSnapshot",
        "pz.bridge.debug.listAvailableMethods",
        "pz.bridge.debug.bridgeSnapshot"
    );

    private static final List<String> REQUIRED_BANK_METHODS = Arrays.asList(
        "pz.bridge.bank.getBalance",
        "pz.bridge.bank.deposit",
        "pz.bridge.bank.withdraw"
    );

    private static volatile boolean initialized;

    private BridgeBootstrap() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            DebugLog.General.debugln("[PZLIFE][BOOT] BridgeBootstrap already initialized");
            return;
        }

        DebugLog.General.debugln("[PZLIFE][BOOT] Initializing BridgeBootstrap...");

        // 1. Initialize loader/runtime registry
        LoaderBootstrap.initialize();

        // 2. Register all bridge methods
        DebugBridge.register();
        DebugLog.General.debugln("[PZLIFE][BOOT] DebugBridge registered");

        BankBridge.register();
        DebugLog.General.debugln("[PZLIFE][BOOT] BankBridge registered");

        // 3. Mark identity lifecycle as ready
        IdentityLifecycleBridge.markReady();
        DebugLog.General.debugln("[PZLIFE][IDENTITY] Java-side identity lifecycle marked ready");

        // 4. Start identity discovery watcher (polls for local player)
        IdentityDiscoveryWatcher.start();
        DebugLog.General.debugln("[PZLIFE][IDENTITY] Java-side identity detector armed");

        // 5. Validate required bridge methods exist
        validateBridgeSurface();

        initialized = true;

        DebugLog.General.debugln(
            "[PZLIFE][BOOT] BridgeBootstrap.initialize() completed with methods=" + ModuleRegistry.count()
        );
    }

    public static boolean isInitialized() {
        return initialized;
    }

    private static void validateBridgeSurface() {
        validateMethods("debug", REQUIRED_DEBUG_METHODS);
        validateMethods("bank", REQUIRED_BANK_METHODS);
    }

    private static void validateMethods(String groupName, List<String> methods) {
        for (String methodName : methods) {
            if (!ModuleRegistry.has(methodName)) {
                throw new IllegalStateException(
                    "Required " + groupName + " bridge method missing after bootstrap: " + methodName
                );
            }
        }
    }
}