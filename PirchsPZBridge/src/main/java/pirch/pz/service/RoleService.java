package pirch.pz.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import pirch.pz.db.DatabaseManager;
import pirch.pz.repo.PostgresAccountRepository;
import pirch.pz.repo.PostgresOwnershipRepository;
import pirch.pz.repo.PostgresPermissionRepository;
import pirch.pz.repo.PostgresRoleRepository;

public final class RoleService {
    private RoleService() {
    }

    public static void ensureCoreRoles(Connection connection) throws SQLException {
        PostgresRoleRepository.ensureRole(connection, "admin", "Administrator");
        PostgresRoleRepository.ensureRole(connection, "mechanic", "Mechanic");
        PostgresRoleRepository.ensureRole(connection, "police", "Police");
        PostgresRoleRepository.ensureRole(connection, "business_manager", "Business Manager");
        PostgresRoleRepository.ensureRole(connection, "keyholder", "Keyholder");

        PostgresRoleRepository.ensureRolePermission(connection, "admin", AuthorizationPolicy.PERMISSION_MANAGE, null, null);
        PostgresRoleRepository.ensureRolePermission(connection, "admin", AuthorizationPolicy.ROLE_ASSIGN, null, null);
        PostgresRoleRepository.ensureRolePermission(connection, "admin", "system.admin", null, null);

        PostgresRoleRepository.ensureRolePermission(connection, "mechanic", "ui.mechanic.open", null, null);
        PostgresRoleRepository.ensureRolePermission(connection, "police", "ui.police.open", null, null);

        PostgresRoleRepository.ensureRolePermission(connection, "business_manager", "business.manage", null, null);
        PostgresRoleRepository.ensureRolePermission(connection, "business_manager", "staff.manage", null, null);
        PostgresRoleRepository.ensureRolePermission(connection, "business_manager", "inventory.manage", null, null);

        PostgresRoleRepository.ensureRolePermission(connection, "keyholder", "vehicle.use", null, null);
        PostgresRoleRepository.ensureRolePermission(connection, "keyholder", "vehicle.drive", null, null);
        PostgresRoleRepository.ensureRolePermission(connection, "keyholder", "vehicle.access_storage", null, null);
    }

    public static Map<String, Object> assign(
        PlayerIdentity actor,
        PlayerIdentity target,
        String roleKey,
        String scopeType,
        String scopeKey
    ) {
        try (Connection connection = DatabaseManager.getConnection()) {
            int actorAccountId = PostgresAccountRepository.resolveOrCreateAccount(actor);
            int targetAccountId = PostgresAccountRepository.resolveOrCreateAccount(target);

            authorizeAssign(connection, actorAccountId, scopeType, scopeKey);

            return PostgresRoleRepository.assignRole(
                connection,
                targetAccountId,
                roleKey,
                scopeType,
                scopeKey,
                actorAccountId
            );
        } catch (SQLException e) {
            throw new RuntimeException("assign role failed", e);
        }
    }

    public static boolean revoke(
        PlayerIdentity actor,
        PlayerIdentity target,
        String roleKey,
        String scopeType,
        String scopeKey
    ) {
        try (Connection connection = DatabaseManager.getConnection()) {
            int actorAccountId = PostgresAccountRepository.resolveOrCreateAccount(actor);
            int targetAccountId = PostgresAccountRepository.resolveOrCreateAccount(target);

            authorizeAssign(connection, actorAccountId, scopeType, scopeKey);

            return PostgresRoleRepository.revokeRole(connection, targetAccountId, roleKey, scopeType, scopeKey);
        } catch (SQLException e) {
            throw new RuntimeException("revoke role failed", e);
        }
    }

    public static boolean has(PlayerIdentity target, String roleKey, String scopeType, String scopeKey) {
        try (Connection connection = DatabaseManager.getConnection()) {
            int targetAccountId = PostgresAccountRepository.resolveOrCreateAccount(target);
            return PostgresRoleRepository.hasRole(connection, targetAccountId, roleKey, scopeType, scopeKey);
        } catch (SQLException e) {
            throw new RuntimeException("has role failed", e);
        }
    }

    public static List<Map<String, Object>> list(PlayerIdentity target) {
        try (Connection connection = DatabaseManager.getConnection()) {
            int targetAccountId = PostgresAccountRepository.resolveOrCreateAccount(target);
            return PostgresRoleRepository.listRoles(connection, targetAccountId);
        } catch (SQLException e) {
            throw new RuntimeException("list roles failed", e);
        }
    }

    public static boolean roleImpliesPermission(
        Connection connection,
        int accountId,
        String permissionKey,
        String scopeType,
        String scopeKey
    ) throws SQLException {
        return PostgresRoleRepository.roleImpliesPermission(connection, accountId, permissionKey, scopeType, scopeKey);
    }

    public static void assignRole(Connection connection, int accountId, String roleKey, Integer grantedByAccountId)
        throws SQLException {
        PostgresRoleRepository.assignRole(connection, accountId, roleKey, null, null, grantedByAccountId);
    }

    private static void authorizeAssign(Connection connection, int actorAccountId, String scopeType, String scopeKey)
        throws SQLException {
        if (PostgresPermissionRepository.hasDirect(connection, actorAccountId, AuthorizationPolicy.ROLE_ASSIGN, null, null)
            || PostgresRoleRepository.roleImpliesPermission(connection, actorAccountId, AuthorizationPolicy.ROLE_ASSIGN, null, null)) {
            return;
        }

        if (AuthorizationPolicy.isNodeScope(scopeType, scopeKey)
            && (PostgresPermissionRepository.hasDirect(connection, actorAccountId, AuthorizationPolicy.ROLE_ASSIGN_SCOPE, scopeType, scopeKey)
                || PostgresRoleRepository.roleImpliesPermission(connection, actorAccountId, AuthorizationPolicy.ROLE_ASSIGN_SCOPE, scopeType, scopeKey)
                || PostgresOwnershipRepository.isOwner(connection, actorAccountId, scopeType, scopeKey))) {
            return;
        }

        throw new IllegalStateException("actor is not authorized to assign this role");
    }
}
