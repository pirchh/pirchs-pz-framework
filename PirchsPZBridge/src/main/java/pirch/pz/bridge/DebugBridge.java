package pirch.pz.bridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pirch.pz.debug.IdentityLifecycleBridge;
import pirch.pz.service.AuthSelfTestService;
import pirch.pz.service.IdentityLifecycleService;
import pirch.pz.service.OwnershipService;
import pirch.pz.service.PermissionService;
import pirch.pz.service.PlayerIdentity;
import pirch.pz.service.RoleService;
import pirch.pzloader.bootstrap.LoaderBootstrap;
import pirch.pzloader.runtime.BridgeMethodDefinition;
import pirch.pzloader.runtime.BridgeResult;
import pirch.pzloader.runtime.ModuleRegistry;

public final class DebugBridge {
    private static volatile boolean registered;

    private DebugBridge() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }

        registerMethod(
            "pz.bridge.debug.isLocalIdentityReady",
            "Returns true when the local account identity has been resolved and debug methods can run",
            0,
            args -> BridgeResult.ok(isLocalIdentityReady())
        );
        registerMethod(
            "pz.bridge.identity.isReady",
            "Stable alias for local identity readiness",
            0,
            args -> BridgeResult.ok(isLocalIdentityReady())
        );

        registerMethod(
            "pz.bridge.debug.selfTestNow",
            "Runs the auth self-test immediately for the resolved local account",
            0,
            args -> BridgeResult.ok(AuthSelfTestService.runNow())
        );
        registerMethod(
            "pz.bridge.debug.selfTestStatus",
            "Returns the last auth self-test status snapshot",
            0,
            args -> BridgeResult.ok(AuthSelfTestService.getStatus())
        );

        registerMethod(
            "pz.bridge.debug.runSmokeSuite",
            "Runs a focused manual smoke suite for the resolved local account",
            1,
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

        registerMethod(
            "pz.bridge.debug.resetLifecycle",
            "Resets local lifecycle state and auth self-test state for debug retesting",
            0,
            args -> {
                IdentityLifecycleService.resetLocalResolution("manual debug reset");
                return BridgeResult.ok(snapshot("manual debug reset"));
            }
        );
        registerMethod(
            "pz.bridge.identity.reset",
            "Stable alias for lifecycle reset",
            0,
            args -> {
                IdentityLifecycleService.resetLocalResolution("manual stable reset");
                return BridgeResult.ok(snapshot("manual stable reset"));
            }
        );

        registerMethod(
            "pz.bridge.debug.claimNode",
            "Claims a node for the resolved local account",
            1,
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
        registerMethod(
            "pz.bridge.ownership.claimNode",
            "Stable alias for claiming a node for the resolved local account",
            1,
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

        registerMethod(
            "pz.bridge.debug.releaseNode",
            "Releases a node for the resolved local account",
            1,
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
        registerMethod(
            "pz.bridge.ownership.releaseNode",
            "Stable alias for releasing a node",
            1,
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

        registerMethod(
            "pz.bridge.debug.getNodeOwner",
            "Returns the current owner of a node",
            1,
            args -> {
                try {
                    String nodeKey = String.valueOf(args[0]).trim();
                    return BridgeResult.ok(OwnershipService.getNodeOwner(nodeKey));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );
        registerMethod(
            "pz.bridge.ownership.getNodeOwner",
            "Stable alias for getting current node owner",
            1,
            args -> {
                try {
                    String nodeKey = String.valueOf(args[0]).trim();
                    return BridgeResult.ok(OwnershipService.getNodeOwner(nodeKey));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        registerMethod(
            "pz.bridge.debug.listOwnedNodes",
            "Lists nodes owned by the resolved local account",
            0,
            args -> {
                try {
                    return BridgeResult.ok(OwnershipService.listOwnedNodes(requireLocalIdentity()));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );
        registerMethod(
            "pz.bridge.ownership.listMine",
            "Stable alias for listing nodes owned by the resolved local account",
            0,
            args -> {
                try {
                    return BridgeResult.ok(OwnershipService.listOwnedNodes(requireLocalIdentity()));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        registerMethod(
            "pz.bridge.debug.grantPermissionToSelf",
            "Grants a global or scoped permission to the resolved local account using itself as actor",
            1,
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
        registerMethod(
            "pz.bridge.permission.grantToSelf",
            "Stable alias for granting a permission to the resolved local account",
            1,
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

        registerMethod(
            "pz.bridge.debug.revokePermissionFromSelf",
            "Revokes a global or scoped permission from the resolved local account using itself as actor",
            1,
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
        registerMethod(
            "pz.bridge.permission.revokeFromSelf",
            "Stable alias for revoking a permission from the resolved local account",
            1,
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

        registerMethod(
            "pz.bridge.debug.hasPermission",
            "Checks whether the resolved local account has a permission",
            1,
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
        registerMethod(
            "pz.bridge.permission.has",
            "Stable alias for checking whether the resolved local account has a permission",
            1,
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

        registerMethod(
            "pz.bridge.debug.explainPermission",
            "Explains permission resolution for the resolved local account",
            1,
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
        registerMethod(
            "pz.bridge.permission.explain",
            "Stable alias for permission resolution explanation",
            1,
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

        registerMethod(
            "pz.bridge.debug.listPermissions",
            "Lists permissions for the resolved local account",
            0,
            args -> {
                try {
                    return BridgeResult.ok(PermissionService.list(requireLocalIdentity()));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );
        registerMethod(
            "pz.bridge.permission.list",
            "Stable alias for listing permissions for the resolved local account",
            0,
            args -> {
                try {
                    return BridgeResult.ok(PermissionService.list(requireLocalIdentity()));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        registerMethod(
            "pz.bridge.debug.assignRoleToSelf",
            "Assigns a global or scoped role to the resolved local account using itself as actor",
            1,
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
        registerMethod(
            "pz.bridge.role.assignToSelf",
            "Stable alias for assigning a role to the resolved local account",
            1,
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

        registerMethod(
            "pz.bridge.debug.revokeRoleFromSelf",
            "Revokes a global or scoped role from the resolved local account using itself as actor",
            1,
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
        registerMethod(
            "pz.bridge.role.revokeFromSelf",
            "Stable alias for revoking a role from the resolved local account",
            1,
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

        registerMethod(
            "pz.bridge.debug.hasRole",
            "Checks whether the resolved local account has a role",
            1,
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
        registerMethod(
            "pz.bridge.role.has",
            "Stable alias for checking whether the resolved local account has a role",
            1,
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

        registerMethod(
            "pz.bridge.debug.listRoles",
            "Lists roles for the resolved local account",
            0,
            args -> {
                try {
                    return BridgeResult.ok(RoleService.list(requireLocalIdentity()));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );
        registerMethod(
            "pz.bridge.role.list",
            "Stable alias for listing roles for the resolved local account",
            0,
            args -> {
                try {
                    return BridgeResult.ok(RoleService.list(requireLocalIdentity()));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        registerMethod(
            "pz.bridge.debug.localSnapshot",
            "Returns the currently resolved local identity and auth self-test snapshot",
            0,
            args -> BridgeResult.ok(snapshot("manual snapshot"))
        );
        registerMethod(
            "pz.bridge.identity.snapshot",
            "Stable alias for local identity snapshot",
            0,
            args -> BridgeResult.ok(snapshot("manual stable snapshot"))
        );

        registerMethod(
            "pz.bridge.debug.listAvailableMethods",
            "Lists all currently registered bridge method names",
            0,
            args -> BridgeResult.ok(new ArrayList<>(ModuleRegistry.listMethodNames()))
        );
        registerMethod(
            "pz.bridge.runtime.listMethods",
            "Stable alias for listing all currently registered bridge methods",
            0,
            args -> BridgeResult.ok(new ArrayList<>(ModuleRegistry.listMethodNames()))
        );

        registerMethod(
            "pz.bridge.debug.bridgeSnapshot",
            "Returns loader and bridge registration state for runtime diagnostics",
            0,
            args -> BridgeResult.ok(bridgeSnapshot())
        );
        registerMethod(
            "pz.bridge.runtime.snapshot",
            "Stable alias for runtime bridge diagnostics",
            0,
            args -> BridgeResult.ok(bridgeSnapshot())
        );

        registered = true;
    }

    private static void registerMethod(
        String methodName,
        String description,
        int minArgCount,
        java.util.function.Function<Object[], BridgeResult> handler
    ) {
        BridgeMethodDefinition.Builder builder = BridgeMethodDefinition.builder(methodName)
            .description(description);

        if (minArgCount > 0) {
            builder.minArgCount(minArgCount);
        }

        ModuleRegistry.register(builder.build(), handler::apply);
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
        result.put("persistencePriority", "high");
        return result;
    }

    private static Map<String, Object> snapshot(String reason) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reason", reason);
        result.put("ready", isLocalIdentityReady());
        result.put("lifecycle", IdentityLifecycleService.snapshot().toMap());
        result.put("lifecycleDiagnostics", IdentityLifecycleBridge.diagnostics());
        result.put("authSelfTest", AuthSelfTestService.getStatus());

        PlayerIdentity identity = IdentityLifecycleService.getLastIdentity();
        result.put("accountId", IdentityLifecycleService.getLastResolvedAccountId());
        result.put("playerNum", IdentityLifecycleService.getLastResolvedPlayerNum());
        result.put("accountExternalId", identity != null ? identity.getAccountExternalId() : null);
        result.put("identity", identity != null ? identity.toMap() : null);
        result.put("resolutionSource", IdentityLifecycleService.getLastResolutionSource());
        result.put("resolutionAuthoritative", IdentityLifecycleService.isLastResolutionAuthoritative());
        result.put("resolutionEpochMs", IdentityLifecycleService.getLastResolutionEpochMs());
        return result;
    }

    private static Map<String, Object> bridgeSnapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("registered", registered);
        result.put("loader", LoaderBootstrap.snapshot());
        result.put("methodCount", ModuleRegistry.count());
        result.put("identity", IdentityLifecycleBridge.diagnostics());

        Map<String, Map<String, Object>> definitions = new LinkedHashMap<>();
        for (Map.Entry<String, BridgeMethodDefinition> entry : ModuleRegistry.getAllDefinitions().entrySet()) {
            BridgeMethodDefinition definition = entry.getValue();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("version", definition.getVersion());
            data.put("description", definition.getDescription());
            data.put("minArgCount", definition.getMinArgCount());
            definitions.put(entry.getKey(), data);
        }
        result.put("definitions", definitions);
        return result;
    }
}
