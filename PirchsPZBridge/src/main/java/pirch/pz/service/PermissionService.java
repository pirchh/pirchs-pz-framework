package pirch.pz.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import pirch.pz.db.DatabaseManager;
import pirch.pz.repo.PostgresAccountRepository;
import pirch.pz.repo.PostgresOwnershipRepository;
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
        try (Connection connection = DatabaseManager.getConnection()) {
            int actorAccountId = PostgresAccountRepository.resolveOrCreateAccount(actor);
            int targetAccountId = PostgresAccountRepository.resolveOrCreateAccount(target);

            authorizeManage(connection, actorAccountId, scopeType, scopeKey);

            return castMap(PostgresPermissionRepository.grant(
                connection,
                targetAccountId,
                permissionKey,
                scopeType,
                scopeKey,
                actorAccountId
            ));
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
            return PostgresPermissionRepository.has(connection, targetAccountId, permissionKey, scopeType, scopeKey);
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
            return castMap(PostgresPermissionRepository.explain(
                connection,
                targetAccountId,
                permissionKey,
                scopeType,
                scopeKey
            ));
        } catch (SQLException e) {
            throw new RuntimeException("explain failed", e);
        }
    }

    private static void authorizeManage(
        Connection connection,
        int actorAccountId,
        String scopeType,
        String scopeKey
    ) throws SQLException {
        if (hasManagePermission(connection, actorAccountId)) {
            return;
        }

        if (canDelegateOwnedScope(connection, actorAccountId, scopeType, scopeKey)) {
            return;
        }

        throw new IllegalStateException("actor is not authorized to manage this permission grant");
    }

    private static boolean hasManagePermission(Connection connection, int actorAccountId) throws SQLException {
        return PostgresPermissionRepository.has(connection, actorAccountId, MANAGE_PERMISSION, null, null);
    }

    private static boolean canDelegateOwnedScope(
        Connection connection,
        int actorAccountId,
        String scopeType,
        String scopeKey
    ) throws SQLException {
        if (scopeType == null || scopeKey == null) {
            return false;
        }

        if (!"node".equalsIgnoreCase(scopeType)) {
            return false;
        }

        return PostgresOwnershipRepository.isOwner(connection, actorAccountId, scopeType, scopeKey)
            && PostgresPermissionRepository.has(connection, actorAccountId, "ownership.delegate", scopeType, scopeKey);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}