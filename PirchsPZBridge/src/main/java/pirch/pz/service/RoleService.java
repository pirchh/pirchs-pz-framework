package pirch.pz.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import pirch.pz.repo.PostgresRoleRepository;

public final class RoleService {
    private RoleService() {}

    public static void ensureCoreRoles(Connection connection) throws SQLException {
        PostgresRoleRepository.ensureRole(connection, "admin", "Administrator");
        PostgresRoleRepository.ensureRole(connection, "mechanic", "Mechanic");
        PostgresRoleRepository.ensureRole(connection, "police", "Police");
        PostgresRoleRepository.ensureRole(connection, "business_manager", "Business Manager");
        PostgresRoleRepository.ensureRolePermission(connection, "admin", "permissions.manage", null, null);
        PostgresRoleRepository.ensureRolePermission(connection, "admin", "roles.assign", null, null);
        PostgresRoleRepository.ensureRolePermission(connection, "admin", "system.admin", null, null);
        PostgresRoleRepository.ensureRolePermission(connection, "mechanic", "ui.mechanic.open", null, null);
        PostgresRoleRepository.ensureRolePermission(connection, "police", "ui.police.open", null, null);
        PostgresRoleRepository.ensureRolePermission(connection, "business_manager", "business.manage", null, null);
    }

    public static void assignRole(Connection connection, int accountId, String roleKey, Integer grantedByAccountId) throws SQLException {
        PostgresRoleRepository.assignRole(connection, accountId, roleKey, grantedByAccountId);
    }

    public static boolean hasRole(Connection connection, int accountId, String roleKey) throws SQLException {
        return PostgresRoleRepository.hasRole(connection, accountId, roleKey);
    }

    public static List<String> listRoles(Connection connection, int accountId) throws SQLException {
        return PostgresRoleRepository.listRoles(connection, accountId);
    }

    public static boolean roleImpliesPermission(Connection connection, int accountId, String permissionKey, String scopeType, String scopeKey) throws SQLException {
        return PostgresRoleRepository.roleImpliesPermission(connection, accountId, permissionKey, scopeType, scopeKey);
    }
}
