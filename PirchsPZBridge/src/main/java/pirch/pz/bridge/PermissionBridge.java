package pirch.pz.bridge;

import pirch.pz.service.PermissionService;
import pirch.pz.service.PlayerIdentity;
import pirch.pz.service.PlayerIdentityService;
import pirch.pzloader.runtime.BridgeMethodDefinition;
import pirch.pzloader.runtime.BridgeResult;
import pirch.pzloader.runtime.ModuleRegistry;

public final class PermissionBridge {
    private PermissionBridge() {
    }

    public static void register() {
        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.permissions.grant")
                .description("Grants a permission to a target account")
                .minArgCount(3)
                .build(),
            args -> {
                try {
                    PlayerIdentity actor = PlayerIdentityService.fromBridgeArg(args[0]);
                    PlayerIdentity target = PlayerIdentityService.fromBridgeArg(args[1]);
                    String permissionKey = String.valueOf(args[2]).trim();
                    String scopeType = args.length >= 4 && args[3] != null ? String.valueOf(args[3]).trim() : null;
                    String scopeKey = args.length >= 5 && args[4] != null ? String.valueOf(args[4]).trim() : null;
                    return BridgeResult.ok(PermissionService.grant(actor, target, permissionKey, scopeType, scopeKey));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.permissions.revoke")
                .description("Revokes a permission from a target account")
                .minArgCount(3)
                .build(),
            args -> {
                try {
                    PlayerIdentity actor = PlayerIdentityService.fromBridgeArg(args[0]);
                    PlayerIdentity target = PlayerIdentityService.fromBridgeArg(args[1]);
                    String permissionKey = String.valueOf(args[2]).trim();
                    String scopeType = args.length >= 4 && args[3] != null ? String.valueOf(args[3]).trim() : null;
                    String scopeKey = args.length >= 5 && args[4] != null ? String.valueOf(args[4]).trim() : null;
                    return BridgeResult.ok(PermissionService.revoke(actor, target, permissionKey, scopeType, scopeKey));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.permissions.has")
                .description("Checks whether an account has a permission, including owner-implied access where applicable")
                .minArgCount(2)
                .build(),
            args -> {
                try {
                    PlayerIdentity target = PlayerIdentityService.fromBridgeArg(args[0]);
                    String permissionKey = String.valueOf(args[1]).trim();
                    String scopeType = args.length >= 3 && args[2] != null ? String.valueOf(args[2]).trim() : null;
                    String scopeKey = args.length >= 4 && args[3] != null ? String.valueOf(args[3]).trim() : null;
                    return BridgeResult.ok(PermissionService.has(target, permissionKey, scopeType, scopeKey));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.permissions.list")
                .description("Lists active permission grants for an account")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    PlayerIdentity target = PlayerIdentityService.fromBridgeArg(args[0]);
                    return BridgeResult.ok(PermissionService.list(target));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.permissions.explain")
                .description("Explains why an account is or is not authorized for a permission and scope")
                .minArgCount(2)
                .build(),
            args -> {
                try {
                    PlayerIdentity target = PlayerIdentityService.fromBridgeArg(args[0]);
                    String permissionKey = String.valueOf(args[1]).trim();
                    String scopeType = args.length >= 3 && args[2] != null ? String.valueOf(args[2]).trim() : null;
                    String scopeKey = args.length >= 4 && args[3] != null ? String.valueOf(args[3]).trim() : null;
                    return BridgeResult.ok(PermissionService.explain(target, permissionKey, scopeType, scopeKey));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );
    }
}
