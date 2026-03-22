package pirch.pz.service;

import java.util.List;
import java.util.Map;
import pirch.pz.repo.PostgresPermissionRepository;

public final class PermissionService {
    private static final String MANAGE_PERMISSION = "permissions.manage";

    private PermissionService() {
    }

    public static Map<String, Object> grant(
        PlayerIdentity actor,
        PlayerIdentity target,
        String permissionKey,
        String scopeType,
        String scopeKey
    ) {
        authorizeManage(actor, target, permissionKey);
        return PostgresPermissionRepository.grant(target, permissionKey, scopeType, scopeKey);
    }

    public static boolean revoke(
        PlayerIdentity actor,
        PlayerIdentity target,
        String permissionKey,
        String scopeType,
        String scopeKey
    ) {
        authorizeManage(actor, target, permissionKey);
        return PostgresPermissionRepository.revoke(target, permissionKey, scopeType, scopeKey);
    }

    public static boolean has(PlayerIdentity target, String permissionKey, String scopeType, String scopeKey) {
        return PostgresPermissionRepository.has(target, permissionKey, scopeType, scopeKey);
    }

    public static List<Map<String, Object>> list(PlayerIdentity target) {
        return PostgresPermissionRepository.list(target);
    }

    private static void authorizeManage(PlayerIdentity actor, PlayerIdentity target, String permissionKey) {
        PlayerIdentityService.validate(actor);
        PlayerIdentityService.validate(target);

        if (actor.getAccountExternalId().equals(target.getAccountExternalId())
            && "ownership.use".equals(permissionKey)) {
            return;
        }

        if (!PostgresPermissionRepository.has(actor, MANAGE_PERMISSION, null, null)) {
            throw new IllegalStateException("actor does not have permissions.manage");
        }
    }
}
