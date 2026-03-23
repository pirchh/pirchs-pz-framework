package pirch.pz.bridge;

import pirch.pz.service.OwnershipService;
import pirch.pz.service.PlayerIdentity;
import pirch.pz.service.PlayerIdentityService;
import pirch.pzloader.runtime.BridgeMethodDefinition;
import pirch.pzloader.runtime.BridgeResult;
import pirch.pzloader.runtime.ModuleRegistry;

public final class OwnershipBridge {
    private OwnershipBridge() {
    }

    public static void register() {
        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.ownership.claimNode")
                .description("Claims a world node for an account")
                .minArgCount(2)
                .build(),
            args -> {
                try {
                    PlayerIdentity identity = PlayerIdentityService.fromBridgeArg(args[0]);
                    String nodeKey = String.valueOf(args[1]).trim();
                    String nodeType = args.length >= 3 && args[2] != null
                        ? String.valueOf(args[2]).trim()
                        : "generic";
                    return BridgeResult.ok(OwnershipService.claimNode(identity, nodeKey, nodeType));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.ownership.releaseNode")
                .description("Releases a claimed world node")
                .minArgCount(2)
                .build(),
            args -> {
                try {
                    PlayerIdentity identity = PlayerIdentityService.fromBridgeArg(args[0]);
                    String nodeKey = String.valueOf(args[1]).trim();
                    return BridgeResult.ok(OwnershipService.releaseNode(identity, nodeKey));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.ownership.getNodeOwner")
                .description("Returns the current owner of a world node")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    return BridgeResult.ok(OwnershipService.getNodeOwner(String.valueOf(args[0]).trim()));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.ownership.listOwnedNodes")
                .description("Lists nodes owned by an account")
                .minArgCount(1)
                .build(),
            args -> {
                try {
                    PlayerIdentity identity = PlayerIdentityService.fromBridgeArg(args[0]);
                    return BridgeResult.ok(OwnershipService.listOwnedNodes(identity));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );

        ModuleRegistry.register(
            BridgeMethodDefinition.builder("pz.bridge.ownership.isOwner")
                .description("Checks whether an account currently owns a node")
                .minArgCount(2)
                .build(),
            args -> {
                try {
                    PlayerIdentity identity = PlayerIdentityService.fromBridgeArg(args[0]);
                    String nodeKey = String.valueOf(args[1]).trim();
                    return BridgeResult.ok(OwnershipService.isOwner(identity, nodeKey));
                } catch (Exception e) {
                    return BridgeResult.fail(e.getMessage());
                }
            }
        );
    }
}
