package pirch.pz.bridge;

import java.util.LinkedHashMap;
import java.util.Map;
import pirch.pz.service.AuthSelfTestService;
import pirch.pz.service.IdentityLifecycleService;
import pirch.pz.service.OwnershipService;
import pirch.pz.service.PermissionService;
import pirch.pz.service.PlayerIdentity;
import pirch.pz.service.RoleService;
import pirch.pzloader.runtime.BridgeMethodDefinition;
import pirch.pzloader.runtime.BridgeResult;
import pirch.pzloader.runtime.ModuleRegistry;

public final class DebugBridge {
    private DebugBridge() {
    }

    public static void register() {
        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.isLocalIdentityReady")
                .description("Returns true when the local account identity has been resolved and debug methods can run")
                .build(),
            args -> BridgeResult.ok(isLocalIdentityReady())
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.selfTestNow")
                .description("Runs the auth self-test immediately for the resolved local account")
                .build(),
            args -> BridgeResult.ok(AuthSelfTestService.runNow())
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.selfTestStatus")
                .description("Returns the last auth self-test status snapshot")
                .build(),
            args -> BridgeResult.ok(AuthSelfTestService.getStatus())
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.runSmokeSuite")
                .description("Runs a focused manual smoke suite for the resolved local account")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    PlayerIdentity identity = requireLocalIdentity();
                    String nodeKey = String.valueOf(args[0]).trim();
                    String nodeType = args.length >= 2 && args[1] != null
                        ? String.valueOf(args[1]).trim()
                        : "node";
                    return BridgeResult.ok(runSmokeSuite(identity, nodeKey, nodeType));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.resetLifecycle")
                .description("Resets local lifecycle state and auth self-test state for debug retesting")
                .build(),
            args -> {
                IdentityLifecycleService.resetLocalResolution("manual debug reset");
                return BridgeResult.ok(snapshot("manual debug reset"));
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.claimNode")
                .description("Claims a node for the resolved local account")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    PlayerIdentity identity = requireLocalIdentity();
                    String nodeKey = String.valueOf(args[0]).trim();
                    String nodeType = args.length >= 2 && args[1] != null
                        ? String.valueOf(args[1]).trim()
                        : "node";
                    return BridgeResult.ok(OwnershipService.claimNode(identity, nodeKey, nodeType));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.releaseNode")
                .description("Releases a node for the resolved local account")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    PlayerIdentity identity = requireLocalIdentity();
                    String nodeKey = String.valueOf(args[0]).trim();
                    return BridgeResult.ok(OwnershipService.releaseNode(identity, nodeKey));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.getNodeOwner")
                .description("Returns the current owner of a node")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    String nodeKey = String.valueOf(args[0]).trim();
                    return BridgeResult.ok(OwnershipService.getNodeOwner(nodeKey));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.listOwnedNodes")
                .description("Lists nodes owned by the resolved local account")
                .build(),
            args -> {
                try {
                    return BridgeResult.ok(OwnershipService.listOwnedNodes(requireLocalIdentity()));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.grantPermissionToSelf")
                .description("Grants a global or scoped permission to the resolved local account using itself as actor")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    PlayerIdentity identity = requireLocalIdentity();
                    String permissionKey = String.valueOf(args[0]).trim();
                    String scopeType = args.length >= 2 && args[1] != null ? String.valueOf(args[1]).trim() : null;
                    String scopeKey = args.length >= 3 && args[2] != null ? String.valueOf(args[2]).trim() : null;
                    return BridgeResult.ok(PermissionService.grant(identity, identity, permissionKey, scopeType, scopeKey));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.revokePermissionFromSelf")
                .description("Revokes a global or scoped permission from the resolved local account using itself as actor")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    PlayerIdentity identity = requireLocalIdentity();
                    String permissionKey = String.valueOf(args[0]).trim();
                    String scopeType = args.length >= 2 && args[1] != null ? String.valueOf(args[1]).trim() : null;
                    String scopeKey = args.length >= 3 && args[2] != null ? String.valueOf(args[2]).trim() : null;
                    return BridgeResult.ok(PermissionService.revoke(identity, identity, permissionKey, scopeType, scopeKey));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.hasPermission")
                .description("Checks whether the resolved local account has a permission")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    PlayerIdentity identity = requireLocalIdentity();
                    String permissionKey = String.valueOf(args[0]).trim();
                    String scopeType = args.length >= 2 && args[1] != null ? String.valueOf(args[1]).trim() : null;
                    String scopeKey = args.length >= 3 && args[2] != null ? String.valueOf(args[2]).trim() : null;
                    return BridgeResult.ok(PermissionService.has(identity, permissionKey, scopeType, scopeKey));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.explainPermission")
                .description("Explains permission resolution for the resolved local account")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    PlayerIdentity identity = requireLocalIdentity();
                    String permissionKey = String.valueOf(args[0]).trim();
                    String scopeType = args.length >= 2 && args[1] != null ? String.valueOf(args[1]).trim() : null;
                    String scopeKey = args.length >= 3 && args[2] != null ? String.valueOf(args[2]).trim() : null;
                    return BridgeResult.ok(PermissionService.explain(identity, permissionKey, scopeType, scopeKey));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.listPermissions")
                .description("Lists permissions for the resolved local account")
                .build(),
            args -> {
                try {
                    return BridgeResult.ok(PermissionService.list(requireLocalIdentity()));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.assignRoleToSelf")
                .description("Assigns a global or scoped role to the resolved local account using itself as actor")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    PlayerIdentity identity = requireLocalIdentity();
                    String roleKey = String.valueOf(args[0]).trim();
                    String scopeType = args.length >= 2 && args[1] != null ? String.valueOf(args[1]).trim() : null;
                    String scopeKey = args.length >= 3 && args[2] != null ? String.valueOf(args[2]).trim() : null;
                    return BridgeResult.ok(RoleService.assign(identity, identity, roleKey, scopeType, scopeKey));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.revokeRoleFromSelf")
                .description("Revokes a global or scoped role from the resolved local account using itself as actor")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    PlayerIdentity identity = requireLocalIdentity();
                    String roleKey = String.valueOf(args[0]).trim();
                    String scopeType = args.length >= 2 && args[1] != null ? String.valueOf(args[1]).trim() : null;
                    String scopeKey = args.length >= 3 && args[2] != null ? String.valueOf(args[2]).trim() : null;
                    return BridgeResult.ok(RoleService.revoke(identity, identity, roleKey, scopeType, scopeKey));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.hasRole")
                .description("Checks whether the resolved local account has a role")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    PlayerIdentity identity = requireLocalIdentity();
                    String roleKey = String.valueOf(args[0]).trim();
                    String scopeType = args.length >= 2 && args[1] != null ? String.valueOf(args[1]).trim() : null;
                    String scopeKey = args.length >= 3 && args[2] != null ? String.valueOf(args[2]).trim() : null;
                    return BridgeResult.ok(RoleService.has(identity, roleKey, scopeType, scopeKey));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.listRoles")
                .description("Lists roles for the resolved local account")
                .build(),
            args -> {
                try {
                    return BridgeResult.ok(RoleService.list(requireLocalIdentity()));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.debug.localSnapshot")
                .description("Returns the currently resolved local identity and auth self-test snapshot")
                .build(),
            args -> BridgeResult.ok(snapshot("manual snapshot"))
        );
    }

    private static boolean isLocalIdentityReady() {
        PlayerIdentity identity = IdentityLifecycleService.getLastIdentity();
        return identity != null
            && identity.getAccountExternalId() != null
            && !identity.getAccountExternalId().isBlank();
    }

    private static PlayerIdentity requireLocalIdentity() {
        PlayerIdentity identity = IdentityLifecycleService.getLastIdentity();
        if (identity == null || identity.getAccountExternalId() == null || identity.getAccountExternalId().isBlank()) {
            throw new IllegalStateException("no local account has been resolved yet");
        }
        return identity;
    }

    private static Map<String, Object> runSmokeSuite(PlayerIdentity identity, String nodeKey, String nodeType) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodeKey", nodeKey);
        result.put("nodeType", nodeType);
        result.put("localSnapshot", snapshot("smoke suite"));
        result.put("claim", OwnershipService.claimNode(identity, nodeKey, nodeType));
        result.put("ownedNodes", OwnershipService.listOwnedNodes(identity));
        result.put("ownerImpliedExplain", PermissionService.explain(identity, "ownership.manage", "node", nodeKey));
        result.put("negativeExplain", PermissionService.explain(identity, "business.manage", "node", nodeKey));
        result.put("grantMechanic", PermissionService.grant(identity, identity, "ui.mechanic.open", null, null));
        result.put("hasMechanic", PermissionService.has(identity, "ui.mechanic.open", null, null));
        result.put("assignMechanicRole", RoleService.assign(identity, identity, "mechanic", null, null));
        result.put("hasMechanicRole", RoleService.has(identity, "mechanic", null, null));
        result.put("roles", RoleService.list(identity));
        return result;
    }

    private static Map<String, Object> snapshot(String reason) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reason", reason);
        result.put("ready", isLocalIdentityReady());
        result.put("lifecycle", IdentityLifecycleService.snapshot().toMap());
        result.put("authSelfTest", AuthSelfTestService.getStatus());

        PlayerIdentity identity = IdentityLifecycleService.getLastIdentity();
        result.put("accountId", IdentityLifecycleService.getLastResolvedAccountId());
        result.put("playerNum", IdentityLifecycleService.getLastResolvedPlayerNum());
        result.put("accountExternalId", identity != null ? identity.getAccountExternalId() : null);
        result.put("identity", identity != null ? identity.toMap() : null);
        return result;
    }
}
