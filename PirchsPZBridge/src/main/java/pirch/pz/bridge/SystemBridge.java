package pirch.pz.bridge;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import pirch.pz.db.DatabaseManager;
import pirch.pz.service.AccountService;
import pirch.pz.service.IdentityLifecycleService;
import pirch.pz.service.PlayerIdentity;
import pirch.pz.service.PlayerIdentityService;
import pirch.pzloader.runtime.BridgeMethodDefinition;
import pirch.pzloader.runtime.BridgeResult;
import pirch.pzloader.runtime.ModuleRegistry;

public final class SystemBridge {
    private static final String BRIDGE_VERSION = "PirchsPZBridge v0.0.3";

    private SystemBridge() {
    }

    public static void register() {
        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.system.ping")
                .description("Simple bridge heartbeat endpoint")
                .build(),
            args -> BridgeResult.ok("pong")
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.system.version")
                .description("Returns bridge version information")
                .build(),
            args -> BridgeResult.ok(BRIDGE_VERSION)
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.system.healthCheck")
                .description("Returns bridge runtime health summary")
                .build(),
            args -> healthCheck()
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.system.listMethods")
                .description("Lists registered bridge methods and metadata")
                .build(),
            args -> listMethods()
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.system.dbPing")
                .description("Tests database connectivity")
                .build(),
            args -> dbPing()
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.system.resolveAccount")
                .description("Resolves or creates an account using a structured player identity or legacy external id")
                .minArgCount(1)
                .build(),
            SystemBridge::resolveAccount
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.player.resolveIdentity")
                .description("Normalizes a structured java-side player identity payload into a canonical form")
                .minArgCount(1)
                .build(),
            SystemBridge::resolvePlayerIdentity
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.player.getLifecycleState")
                .description("Returns current identity lifecycle state")
                .build(),
            args -> BridgeResult.ok(IdentityLifecycleService.snapshot().toMap())
        );
    }

    private static BridgeResult healthCheck() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bridgeVersion", BRIDGE_VERSION);
        data.put("registeredMethodCount", ModuleRegistry.count());
        data.put("hasDbPing", ModuleRegistry.has("pz.bridge.system.dbPing"));
        data.put("hasResolveAccount", ModuleRegistry.has("pz.bridge.system.resolveAccount"));
        data.put("hasResolvePlayerIdentity", ModuleRegistry.has("pz.bridge.player.resolveIdentity"));
        data.put("hasOwnershipClaim", ModuleRegistry.has("pz.bridge.ownership.claimNode"));
        data.put("hasPermissionGrant", ModuleRegistry.has("pz.bridge.permissions.grant"));
        data.put("lifecycle", IdentityLifecycleService.snapshot().toMap());
        return BridgeResult.ok(data);
    }

    private static BridgeResult listMethods() {
        var methods = ModuleRegistry.getAllDefinitions().values().stream()
            .map(definition -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("methodName", definition.getMethodName());
                row.put("version", definition.getVersion());
                row.put("minArgCount", definition.getMinArgCount());
                row.put("description", definition.getDescription());
                return row;
            })
            .collect(Collectors.toList());

        return BridgeResult.ok(methods);
    }

    private static BridgeResult dbPing() {
        try (var connection = DatabaseManager.getConnection()) {
            return BridgeResult.ok("db-ok");
        } catch (Exception e) {
            return BridgeResult.fail(e.getMessage());
        }
    }

    private static BridgeResult resolveAccount(Object... args) {
        try {
            PlayerIdentity identity;
            if (args.length >= 2 && !(args[0] instanceof Map) && !(args[0] instanceof PlayerIdentity)) {
                identity = PlayerIdentity.builder()
                    .playerSource("legacy")
                    .sourcePlayerId(String.valueOf(args[0]).trim())
                    .username(String.valueOf(args[0]).trim())
                    .displayName(args[1] == null ? null : String.valueOf(args[1]).trim())
                    .build();
            } else {
                identity = PlayerIdentityService.fromBridgeArg(args[0]);
            }

            int accountId = AccountService.resolveOrCreateAccount(identity);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("accountId", accountId);
            data.putAll(identity.toMap());
            data.put("accountName", identity.getPreferredAccountName());
            return BridgeResult.ok(data);
        } catch (Exception e) {
            return BridgeResult.fail(e.getMessage());
        }
    }

    private static BridgeResult resolvePlayerIdentity(Object... args) {
        try {
            PlayerIdentity identity = PlayerIdentityService.fromBridgeArg(args[0]);
            return BridgeResult.ok(identity.toMap());
        } catch (Exception e) {
            return BridgeResult.fail(e.getMessage());
        }
    }
}
