package pirch.pz.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pirch.pz.db.DatabaseManager;
import pirch.pz.repo.PostgresAccountRepository;
import pirch.pz.repo.PostgresOwnershipRepository;
import pirch.pz.repo.PostgresPermissionRepository;
import pirch.pz.repo.PostgresRoleRepository;

public final class PermissionService {
    private PermissionService() {
    }

    public static Map<String, Object> grant(
        PlayerIdentity actor,
        PlayerIdentity target,
        String permissionKey,
        String scopeType,
        String scopeKey
    ) {
        try (Connection connection = DatabaseManager.getConnection()) {
            int actorAccountId = PostgresAccountRepository.resolveOrCreateAccount(actor);
            int targetAccountId = PostgresAccountRepository.resolveOrCreateAccount(target);

            authorizeManage(connection, actorAccountId, scopeType, scopeKey);

            return PostgresPermissionRepository.grant(
                connection,
                targetAccountId,
                permissionKey,
                scopeType,
                scopeKey,
                actorAccountId
            );
        } catch (SQLException e) {
            throw new RuntimeException("grant failed", e);
        }
    }

    public static boolean revoke(
        PlayerIdentity actor,
        PlayerIdentity target,
        String permissionKey,
        String scopeType,
        String scopeKey
    ) {
        try (Connection connection = DatabaseManager.getConnection()) {
            int actorAccountId = PostgresAccountRepository.resolveOrCreateAccount(actor);
            int targetAccountId = PostgresAccountRepository.resolveOrCreateAccount(target);

            authorizeManage(connection, actorAccountId, scopeType, scopeKey);

            return PostgresPermissionRepository.revoke(
                connection,
                targetAccountId,
                permissionKey,
                scopeType,
                scopeKey
            );
        } catch (SQLException e) {
            throw new RuntimeException("revoke failed", e);
        }
    }

    public static boolean has(
        PlayerIdentity target,
        String permissionKey,
        String scopeType,
        String scopeKey
    ) {
        try (Connection connection = DatabaseManager.getConnection()) {
            int targetAccountId = PostgresAccountRepository.resolveOrCreateAccount(target);
            return has(connection, targetAccountId, permissionKey, scopeType, scopeKey);
        } catch (SQLException e) {
            throw new RuntimeException("has failed", e);
        }
    }

    public static List<Map<String, Object>> list(PlayerIdentity target) {
        try (Connection connection = DatabaseManager.getConnection()) {
            int targetAccountId = PostgresAccountRepository.resolveOrCreateAccount(target);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> result =
                (List<Map<String, Object>>) (List<?>) PostgresPermissionRepository.list(connection, targetAccountId);
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("list failed", e);
        }
    }

    public static Map<String, Object> explain(
        PlayerIdentity target,
        String permissionKey,
        String scopeType,
        String scopeKey
    ) {
        try (Connection connection = DatabaseManager.getConnection()) {
            int targetAccountId = PostgresAccountRepository.resolveOrCreateAccount(target);
            return explain(connection, targetAccountId, permissionKey, scopeType, scopeKey);
        } catch (SQLException e) {
            throw new RuntimeException("explain failed", e);
        }
    }

    private static boolean has(
        Connection connection,
        int targetAccountId,
        String permissionKey,
        String scopeType,
        String scopeKey
    ) throws SQLException {
        if (PostgresPermissionRepository.hasDirect(connection, targetAccountId, permissionKey, scopeType, scopeKey)) {
            return true;
        }

        if (RoleService.roleImpliesPermission(connection, targetAccountId, permissionKey, scopeType, scopeKey)) {
            return true;
        }

        return AuthorizationPolicy.isNodeScope(scopeType, scopeKey)
            && AuthorizationPolicy.isOwnerImpliedPermission(permissionKey)
            && PostgresOwnershipRepository.isOwner(connection, targetAccountId, scopeType, scopeKey);
    }

    private static Map<String, Object> explain(
        Connection connection,
        int targetAccountId,
        String permissionKey,
        String scopeType,
        String scopeKey
    ) throws SQLException {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accountId", targetAccountId);
        result.put("permissionKey", AuthorizationPolicy.normalize(permissionKey));
        result.put("scopeType", AuthorizationPolicy.normalize(scopeType));
        result.put("scopeKey", AuthorizationPolicy.normalize(scopeKey));

        if (PostgresPermissionRepository.hasDirect(connection, targetAccountId, permissionKey, scopeType, scopeKey)) {
            result.put("allowed", true);
            result.put("reason", "direct_scoped_grant");
            return result;
        }

        if (PostgresPermissionRepository.hasDirect(connection, targetAccountId, permissionKey, null, null)) {
            result.put("allowed", true);
            result.put("reason", "direct_global_grant");
            return result;
        }

        if (RoleService.roleImpliesPermission(connection, targetAccountId, permissionKey, scopeType, scopeKey)) {
            result.put("allowed", true);
            result.put("reason", "role_grant");
            return result;
        }

        if (AuthorizationPolicy.isNodeScope(scopeType, scopeKey)
            && AuthorizationPolicy.isOwnerImpliedPermission(permissionKey)
            && PostgresOwnershipRepository.isOwner(connection, targetAccountId, scopeType, scopeKey)) {
            result.put("allowed", true);
            result.put("reason", "owner_implied");
            return result;
        }

        result.put("allowed", false);
        result.put("reason", "no_matching_permission");
        return result;
    }

    private static void authorizeManage(
        Connection connection,
        int actorAccountId,
        String scopeType,
        String scopeKey
    ) throws SQLException {
        if (has(connection, actorAccountId, AuthorizationPolicy.PERMISSION_MANAGE, null, null)) {
            return;
        }

        if (AuthorizationPolicy.isNodeScope(scopeType, scopeKey)
            && (has(connection, actorAccountId, AuthorizationPolicy.PERMISSION_MANAGE_SCOPE, scopeType, scopeKey)
                || PostgresOwnershipRepository.isOwner(connection, actorAccountId, scopeType, scopeKey))) {
            return;
        }

        throw new IllegalStateException("actor is not authorized to manage this permission grant");
    }
}
