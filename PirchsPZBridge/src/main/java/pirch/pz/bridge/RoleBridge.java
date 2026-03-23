package pirch.pz.bridge;

import pirch.pz.service.PlayerIdentity;
import pirch.pz.service.PlayerIdentityService;
import pirch.pz.service.RoleService;
import pirch.pzloader.runtime.BridgeMethodDefinition;
import pirch.pzloader.runtime.BridgeResult;
import pirch.pzloader.runtime.ModuleRegistry;

public final class RoleBridge {
    private RoleBridge() {
    }

    public static void register() {
        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.roles.assign")
                .description("Assigns a global or scoped role to a target account")
                .minArgCount(3)
                .build(),
            args -> {
                try {
                    PlayerIdentity actor = PlayerIdentityService.fromBridgeArg(args[0]);
                    PlayerIdentity target = PlayerIdentityService.fromBridgeArg(args[1]);
                    String roleKey = String.valueOf(args[2]).trim();
                    String scopeType = args.length >= 4 && args[3] != null ? String.valueOf(args[3]).trim() : null;
                    String scopeKey = args.length >= 5 && args[4] != null ? String.valueOf(args[4]).trim() : null;
                    return BridgeResult.ok(RoleService.assign(actor, target, roleKey, scopeType, scopeKey));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.roles.revoke")
                .description("Revokes a global or scoped role from a target account")
                .minArgCount(3)
                .build(),
            args -> {
                try {
                    PlayerIdentity actor = PlayerIdentityService.fromBridgeArg(args[0]);
                    PlayerIdentity target = PlayerIdentityService.fromBridgeArg(args[1]);
                    String roleKey = String.valueOf(args[2]).trim();
                    String scopeType = args.length >= 4 && args[3] != null ? String.valueOf(args[3]).trim() : null;
                    String scopeKey = args.length >= 5 && args[4] != null ? String.valueOf(args[4]).trim() : null;
                    return BridgeResult.ok(RoleService.revoke(actor, target, roleKey, scopeType, scopeKey));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.roles.has")
                .description("Checks whether an account has a global or scoped role")
                .minArgCount(2)
                .build(),
            args -> {
                try {
                    PlayerIdentity target = PlayerIdentityService.fromBridgeArg(args[0]);
                    String roleKey = String.valueOf(args[1]).trim();
                    String scopeType = args.length >= 3 && args[2] != null ? String.valueOf(args[2]).trim() : null;
                    String scopeKey = args.length >= 4 && args[3] != null ? String.valueOf(args[3]).trim() : null;
                    return BridgeResult.ok(RoleService.has(target, roleKey, scopeType, scopeKey));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.roles.list")
                .description("Lists active global and scoped roles for an account")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    PlayerIdentity target = PlayerIdentityService.fromBridgeArg(args[0]);
                    return BridgeResult.ok(RoleService.list(target));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );
    }
}
