package pirch.pz.service;

import java.util.List;
import java.util.Map;
import pirch.pz.repo.PostgresOwnershipRepository;

public final class OwnershipService {
    private OwnershipService() {
    }

    public static Map<String, Object> claimNode(PlayerIdentity identity, String nodeKey, String nodeType) {
        PlayerIdentityService.validate(identity);
        return PostgresOwnershipRepository.claimNode(identity, nodeKey, nodeType);
    }

    public static boolean releaseNode(PlayerIdentity identity, String nodeKey) {
        PlayerIdentityService.validate(identity);
        return PostgresOwnershipRepository.releaseNode(identity, nodeKey);
    }

    public static Map<String, Object> getNodeOwner(String nodeKey) {
        return PostgresOwnershipRepository.getNodeOwner(nodeKey);
    }

    public static List<Map<String, Object>> listOwnedNodes(PlayerIdentity identity) {
        PlayerIdentityService.validate(identity);
        return PostgresOwnershipRepository.listOwnedNodes(identity);
    }
}
