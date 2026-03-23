package pirch.pz.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pirch.pz.service.AuthorizationPolicy;

public final class PostgresRoleRepository {
    private PostgresRoleRepository() {
    }

    public static void ensureRole(Connection connection, String roleKey, String displayName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO permissions.role (role_key, display_name, active, created_at, updated_at) " +
                "VALUES (?, ?, TRUE, NOW(), NOW()) " +
                "ON CONFLICT (role_key) DO UPDATE SET display_name = EXCLUDED.display_name, active = TRUE, updated_at = NOW()"
        )) {
            statement.setString(1, roleKey);
            statement.setString(2, displayName);
            statement.executeUpdate();
        }
    }

    public static void ensureRolePermission(
        Connection connection,
        String roleKey,
        String permissionKey,
        String scopeType,
        String scopeKey
    ) throws SQLException {
        String normalizedScopeType = AuthorizationPolicy.normalize(scopeType);
        String normalizedScopeKey = AuthorizationPolicy.normalize(scopeKey);

        int updated = updateRolePermission(
            connection,
            roleKey,
            permissionKey,
            normalizedScopeType,
            normalizedScopeKey,
            true
        );

        if (updated > 0) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO permissions.role_permission " +
                "(role_key, permission_key, scope_type, scope_key, active, created_at, updated_at) " +
                "SELECT ?, ?, ?, ?, TRUE, NOW(), NOW() " +
                "WHERE NOT EXISTS (" +
                "    SELECT 1 " +
                "    FROM permissions.role_permission " +
                "    WHERE role_key = ? " +
                "      AND permission_key = ? " +
                "      AND scope_type IS NOT DISTINCT FROM ? " +
                "      AND scope_key IS NOT DISTINCT FROM ?" +
                ")"
        )) {
            statement.setString(1, roleKey);
            statement.setString(2, permissionKey);
            statement.setString(3, normalizedScopeType);
            statement.setString(4, normalizedScopeKey);
            statement.setString(5, roleKey);
            statement.setString(6, permissionKey);
            statement.setString(7, normalizedScopeType);
            statement.setString(8, normalizedScopeKey);
            statement.executeUpdate();
        }
    }

    public static Map<String, Object> assignRole(
        Connection connection,
        int accountId,
        String roleKey,
        String scopeType,
        String scopeKey,
        Integer grantedByAccountId
    ) throws SQLException {
        updateOrInsertAccountRole(connection, accountId, roleKey, scopeType, scopeKey, grantedByAccountId, true);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("accountId", accountId);
        result.put("roleKey", roleKey);
        result.put("scopeType", AuthorizationPolicy.normalize(scopeType));
        result.put("scopeKey", AuthorizationPolicy.normalize(scopeKey));
        result.put("grantedByAccountId", grantedByAccountId);
        result.put("active", true);
        return result;
    }

    public static boolean revokeRole(
        Connection connection,
        int accountId,
        String roleKey,
        String scopeType,
        String scopeKey
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE permissions.account_role " +
                "SET active = FALSE, updated_at = NOW() " +
                "WHERE account_id = ? AND role_key = ? " +
                "AND ((scope_type IS NULL AND ? IS NULL) OR scope_type = ?) " +
                "AND ((scope_key IS NULL AND ? IS NULL) OR scope_key = ?) " +
                "AND active = TRUE"
        )) {
            String normalizedScopeType = AuthorizationPolicy.normalize(scopeType);
            String normalizedScopeKey = AuthorizationPolicy.normalize(scopeKey);

            statement.setInt(1, accountId);
            statement.setString(2, roleKey);
            statement.setString(3, normalizedScopeType);
            statement.setString(4, normalizedScopeType);
            statement.setString(5, normalizedScopeKey);
            statement.setString(6, normalizedScopeKey);
            return statement.executeUpdate() > 0;
        }
    }

    public static boolean hasRole(
        Connection connection,
        int accountId,
        String roleKey,
        String scopeType,
        String scopeKey
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT 1 " +
                "FROM permissions.account_role " +
                "WHERE account_id = ? AND role_key = ? AND active = TRUE " +
                "AND ((scope_type IS NULL AND scope_key IS NULL) OR (scope_type = ? AND scope_key = ?))"
        )) {
            statement.setInt(1, accountId);
            statement.setString(2, roleKey);
            statement.setString(3, AuthorizationPolicy.normalize(scopeType));
            statement.setString(4, AuthorizationPolicy.normalize(scopeKey));
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static List<Map<String, Object>> listRoles(Connection connection, int accountId) throws SQLException {
        List<Map<String, Object>> roles = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT account_id, role_key, scope_type, scope_key, granted_by_account_id, active, created_at, updated_at " +
                "FROM permissions.account_role " +
                "WHERE account_id = ? AND active = TRUE " +
                "ORDER BY role_key ASC, scope_type ASC NULLS FIRST, scope_key ASC NULLS FIRST"
        )) {
            statement.setInt(1, accountId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("accountId", rs.getInt("account_id"));
                    row.put("roleKey", rs.getString("role_key"));
                    row.put("scopeType", rs.getString("scope_type"));
                    row.put("scopeKey", rs.getString("scope_key"));

                    int grantedBy = rs.getInt("granted_by_account_id");
                    row.put("grantedByAccountId", rs.wasNull() ? null : grantedBy);

                    row.put("active", rs.getBoolean("active"));
                    row.put("createdAt", rs.getTimestamp("created_at"));
                    row.put("updatedAt", rs.getTimestamp("updated_at"));
                    roles.add(row);
                }
            }
        }
        return roles;
    }

    public static boolean roleImpliesPermission(
        Connection connection,
        int accountId,
        String permissionKey,
        String scopeType,
        String scopeKey
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT 1 " +
                "FROM permissions.account_role ar " +
                "JOIN permissions.role_permission rp ON rp.role_key = ar.role_key " +
                "WHERE ar.account_id = ? " +
                "  AND ar.active = TRUE " +
                "  AND rp.active = TRUE " +
                "  AND rp.permission_key = ? " +
                "  AND (" +
                "       (rp.scope_type IS NULL AND rp.scope_key IS NULL) " +
                "       OR (rp.scope_type = ? AND rp.scope_key = ?)" +
                "  )"
        )) {
            statement.setInt(1, accountId);
            statement.setString(2, permissionKey);
            statement.setString(3, AuthorizationPolicy.normalize(scopeType));
            statement.setString(4, AuthorizationPolicy.normalize(scopeKey));
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static int updateRolePermission(
        Connection connection,
        String roleKey,
        String permissionKey,
        String scopeType,
        String scopeKey,
        boolean active
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE permissions.role_permission " +
                "SET active = ?, updated_at = NOW() " +
                "WHERE role_key = ? " +
                "AND permission_key = ? " +
                "AND scope_type IS NOT DISTINCT FROM ? " +
                "AND scope_key IS NOT DISTINCT FROM ?"
        )) {
            statement.setBoolean(1, active);
            statement.setString(2, roleKey);
            statement.setString(3, permissionKey);
            statement.setString(4, scopeType);
            statement.setString(5, scopeKey);
            return statement.executeUpdate();
        }
    }

    private static void updateOrInsertAccountRole(
        Connection connection,
        int accountId,
        String roleKey,
        String scopeType,
        String scopeKey,
        Integer grantedByAccountId,
        boolean active
    ) throws SQLException {
        String normalizedScopeType = AuthorizationPolicy.normalize(scopeType);
        String normalizedScopeKey = AuthorizationPolicy.normalize(scopeKey);

        try (PreparedStatement update = connection.prepareStatement(
            "UPDATE permissions.account_role " +
                "SET active = ?, granted_by_account_id = ?, updated_at = NOW() " +
                "WHERE account_id = ? AND role_key = ? " +
                "AND scope_type IS NOT DISTINCT FROM ? " +
                "AND scope_key IS NOT DISTINCT FROM ?"
        )) {
            update.setBoolean(1, active);
            if (grantedByAccountId == null) {
                update.setObject(2, null);
            } else {
                update.setInt(2, grantedByAccountId);
            }
            update.setInt(3, accountId);
            update.setString(4, roleKey);
            update.setString(5, normalizedScopeType);
            update.setString(6, normalizedScopeKey);

            if (update.executeUpdate() > 0) {
                return;
            }
        }

        try (PreparedStatement insert = connection.prepareStatement(
            "INSERT INTO permissions.account_role " +
                "(account_id, role_key, scope_type, scope_key, granted_by_account_id, active, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())"
        )) {
            insert.setInt(1, accountId);
            insert.setString(2, roleKey);
            insert.setString(3, normalizedScopeType);
            insert.setString(4, normalizedScopeKey);
            if (grantedByAccountId == null) {
                insert.setObject(5, null);
            } else {
                insert.setInt(5, grantedByAccountId);
            }
            insert.setBoolean(6, active);
            insert.executeUpdate();
        }
    }
}
