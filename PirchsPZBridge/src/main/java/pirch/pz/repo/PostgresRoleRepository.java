package pirch.pz.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class PostgresRoleRepository {
    private PostgresRoleRepository() {}

    public static void ensureRole(Connection connection, String roleKey, String displayName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO permissions.role (role_key, display_name) VALUES (?, ?) ON CONFLICT (role_key) DO UPDATE SET display_name = EXCLUDED.display_name")) {
            statement.setString(1, roleKey);
            statement.setString(2, displayName);
            statement.executeUpdate();
        }
    }

    public static void ensureRolePermission(Connection connection, String roleKey, String permissionKey, String scopeType, String scopeKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO permissions.role_permission (role_key, permission_key, scope_type, scope_key) VALUES (?, ?, ?, ?) ON CONFLICT (role_key, permission_key, scope_type, scope_key) DO NOTHING")) {
            statement.setString(1, roleKey);
            statement.setString(2, permissionKey);
            statement.setString(3, scopeType);
            statement.setString(4, scopeKey);
            statement.executeUpdate();
        }
    }

    public static void assignRole(Connection connection, int accountId, String roleKey, Integer grantedByAccountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO permissions.account_role (account_id, role_key, granted_by_account_id) VALUES (?, ?, ?) ON CONFLICT (account_id, role_key) DO NOTHING")) {
            statement.setInt(1, accountId);
            statement.setString(2, roleKey);
            if (grantedByAccountId == null) statement.setObject(3, null); else statement.setInt(3, grantedByAccountId);
            statement.executeUpdate();
        }
    }

    public static boolean hasRole(Connection connection, int accountId, String roleKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM permissions.account_role WHERE account_id = ? AND role_key = ?")) {
            statement.setInt(1, accountId);
            statement.setString(2, roleKey);
            try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
        }
    }

    public static List<String> listRoles(Connection connection, int accountId) throws SQLException {
        List<String> roles = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT role_key FROM permissions.account_role WHERE account_id = ? ORDER BY role_key")) {
            statement.setInt(1, accountId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) roles.add(rs.getString("role_key"));
            }
        }
        return roles;
    }

    public static boolean roleImpliesPermission(Connection connection, int accountId, String permissionKey, String scopeType, String scopeKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT 1 FROM permissions.account_role ar JOIN permissions.role_permission rp ON rp.role_key = ar.role_key WHERE ar.account_id = ? AND rp.permission_key = ? AND ((rp.scope_type IS NULL AND rp.scope_key IS NULL) OR (rp.scope_type = ? AND rp.scope_key = ?))")) {
            statement.setInt(1, accountId);
            statement.setString(2, permissionKey);
            statement.setString(3, scopeType);
            statement.setString(4, scopeKey);
            try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
        }
    }
}
