package pirch.pz.bridge;

import java.util.LinkedHashMap;
import java.util.Map;
import pirch.pz.db.DatabaseManager;
import pirch.pz.service.AccountService;
import pirch.pzloader.runtime.BridgeResult;
import pirch.pzloader.runtime.ModuleRegistry;

public final class SystemBridge {
    private static final String BRIDGE_VERSION = "PirchsPZBridge v0";

    private SystemBridge() {
    }

    public static void register() {
        ModuleRegistry.register("pz.bridge.system.ping", (args) -> BridgeResult.ok("pong"));
        ModuleRegistry.register("pz.bridge.system.version", (args) -> BridgeResult.ok(BRIDGE_VERSION));
        ModuleRegistry.register("pz.bridge.system.healthCheck", (args) -> healthCheck());
        ModuleRegistry.register("pz.bridge.system.selfTest", (args) -> selfTest());
        ModuleRegistry.register("pz.bridge.system.dbPing", (args) -> dbPing());
        ModuleRegistry.register("pz.bridge.system.resolveAccount", (args) -> resolveAccount(args));
    }

    private static BridgeResult healthCheck() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bridgeVersion", BRIDGE_VERSION);
        data.put("registeredMethodCount", ModuleRegistry.count());
        data.put("hasPing", ModuleRegistry.has("pz.bridge.system.ping"));
        data.put("hasVersion", ModuleRegistry.has("pz.bridge.system.version"));
        data.put("hasHealthCheck", ModuleRegistry.has("pz.bridge.system.healthCheck"));
        data.put("hasSelfTest", ModuleRegistry.has("pz.bridge.system.selfTest"));
        data.put("hasDbPing", ModuleRegistry.has("pz.bridge.system.dbPing"));
        data.put("hasResolveAccount", ModuleRegistry.has("pz.bridge.system.resolveAccount"));
        data.put("hasGetBalance", ModuleRegistry.has("pz.bridge.bank.getBalance"));
        return BridgeResult.ok(data);
    }

    private static BridgeResult selfTest() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("systemPingRegistered", ModuleRegistry.has("pz.bridge.system.ping"));
        data.put("systemVersionRegistered", ModuleRegistry.has("pz.bridge.system.version"));
        data.put("dbPingRegistered", ModuleRegistry.has("pz.bridge.system.dbPing"));
        data.put("resolveAccountRegistered", ModuleRegistry.has("pz.bridge.system.resolveAccount"));
        data.put("bankGetBalanceRegistered", ModuleRegistry.has("pz.bridge.bank.getBalance"));

        boolean passed =
            ModuleRegistry.has("pz.bridge.system.ping")
                && ModuleRegistry.has("pz.bridge.system.version")
                && ModuleRegistry.has("pz.bridge.system.dbPing")
                && ModuleRegistry.has("pz.bridge.system.resolveAccount")
                && ModuleRegistry.has("pz.bridge.bank.getBalance");

        data.put("passed", passed);

        if (!passed) {
            return BridgeResult.fail("One or more required bridge methods are not registered");
        }

        return BridgeResult.ok(data);
    }

    private static BridgeResult dbPing() {
        try (var connection = DatabaseManager.getConnection()) {
            return BridgeResult.ok("db-ok");
        } catch (Exception e) {
            return BridgeResult.fail(e.getMessage());
        }
    }

    private static BridgeResult resolveAccount(Object... args) {
        if (args == null || args.length < 1 || args[0] == null) {
            return BridgeResult.fail("Missing externalId");
        }

        String externalId = String.valueOf(args[0]).trim();
        if (externalId.isEmpty()) {
            return BridgeResult.fail("externalId cannot be empty");
        }

        String accountName = externalId;
        if (args.length >= 2 && args[1] != null) {
            String providedName = String.valueOf(args[1]).trim();
            if (!providedName.isEmpty()) {
                accountName = providedName;
            }
        }

        try {
            int accountId = AccountService.resolveOrCreateAccount(externalId, accountName);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("accountId", accountId);
            data.put("externalId", externalId);
            data.put("accountName", accountName);

            return BridgeResult.ok(data);
        } catch (Exception e) {
            return BridgeResult.fail(e.getMessage());
        }
    }
}