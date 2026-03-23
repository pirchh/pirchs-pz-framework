package pirch.pz.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PostgresPermissionRepository {
    private PostgresPermissionRepository() {
    }

    public static Map<String, Object> grant(
        Connection connection,
        int accountId,
        String permissionKey,
        String scopeType,
        String scopeKey,
        Integer grantedByAccountId
    ) throws SQLException {
        String normalizedPermissionKey = normalize(permissionKey);
        String normalizedScopeType = normalize(scopeType);
        String normalizedScopeKey = normalize(scopeKey);

        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO permissions.account_permission " +
                "(account_id, permission_key, scope_type, scope_key, granted_by_account_id) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (account_id, permission_key, scope_type, scope_key) DO NOTHING")) {
            statement.setInt(1, accountId);
            statement.setString(2, normalizedPermissionKey);
            statement.setString(3, normalizedScopeType);
            statement.setString(4, normalizedScopeKey);

            if (grantedByAccountId == null) {
                statement.setObject(5, null);
            } else {
                statement.setInt(5, grantedByAccountId);
            }

            statement.executeUpdate();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("accountId", accountId);
        result.put("permissionKey", normalizedPermissionKey);
        result.put("scopeType", normalizedScopeType);
        result.put("scopeKey", normalizedScopeKey);
        result.put("grantedByAccountId", grantedByAccountId);
        return result;
    }

    public static boolean revoke(
        Connection connection,
        int accountId,
        String permissionKey,
        String scopeType,
        String scopeKey
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "DELETE FROM permissions.account_permission " +
                "WHERE account_id = ? AND permission_key = ? " +
                "AND ((scope_type IS NULL AND ? IS NULL) OR scope_type = ?) " +
                "AND ((scope_key IS NULL AND ? IS NULL) OR scope_key = ?)")) {
            String normalizedPermissionKey = normalize(permissionKey);
            String normalizedScopeType = normalize(scopeType);
            String normalizedScopeKey = normalize(scopeKey);

            statement.setInt(1, accountId);
            statement.setString(2, normalizedPermissionKey);
            statement.setString(3, normalizedScopeType);
            statement.setString(4, normalizedScopeType);
            statement.setString(5, normalizedScopeKey);
            statement.setString(6, normalizedScopeKey);

            return statement.executeUpdate() > 0;
        }
    }

    public static boolean hasDirect(
        Connection connection,
        int accountId,
        String permissionKey,
        String scopeType,
        String scopeKey
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT 1 " +
                "FROM permissions.account_permission " +
                "WHERE account_id = ? AND permission_key = ? " +
                "AND ((scope_type IS NULL AND scope_key IS NULL) OR (scope_type = ? AND scope_key = ?))")) {
            statement.setInt(1, accountId);
            statement.setString(2, normalize(permissionKey));
            statement.setString(3, normalize(scopeType));
            statement.setString(4, normalize(scopeKey));

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static boolean has(
        Connection connection,
        int accountId,
        String permissionKey,
        String scopeType,
        String scopeKey
    ) throws SQLException {
        String normalizedPermissionKey = normalize(permissionKey);
        String normalizedScopeType = normalize(scopeType);
        String normalizedScopeKey = normalize(scopeKey);

        if (hasDirect(connection, accountId, normalizedPermissionKey, normalizedScopeType, normalizedScopeKey)) {
            return true;
        }

        if (normalizedScopeType != null && normalizedScopeKey != null) {
            return hasDirect(connection, accountId, normalizedPermissionKey, null, null);
        }

        return false;
    }

    public static List<Map<String, Object>> list(Connection connection, int accountId) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT account_id, permission_key, scope_type, scope_key, granted_by_account_id " +
                "FROM permissions.account_permission " +
                "WHERE account_id = ? " +
                "ORDER BY permission_key ASC, scope_type ASC NULLS FIRST, scope_key ASC NULLS FIRST")) {
            statement.setInt(1, accountId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("accountId", rs.getInt("account_id"));
                    row.put("permissionKey", rs.getString("permission_key"));
                    row.put("scopeType", rs.getString("scope_type"));
                    row.put("scopeKey", rs.getString("scope_key"));

                    int grantedBy = rs.getInt("granted_by_account_id");
                    row.put("grantedByAccountId", rs.wasNull() ? null : grantedBy);

                    results.add(row);
                }
            }
        }

        return results;
    }

    public static Map<String, Object> explain(
        Connection connection,
        int accountId,
        String permissionKey,
        String scopeType,
        String scopeKey
    ) throws SQLException {
        String normalizedPermissionKey = normalize(permissionKey);
        String normalizedScopeType = normalize(scopeType);
        String normalizedScopeKey = normalize(scopeKey);

        Map<String, Object> result = new HashMap<>();
        result.put("accountId", accountId);
        result.put("permissionKey", normalizedPermissionKey);
        result.put("scopeType", normalizedScopeType);
        result.put("scopeKey", normalizedScopeKey);

        if (hasDirect(connection, accountId, normalizedPermissionKey, normalizedScopeType, normalizedScopeKey)) {
            result.put("allowed", true);
            result.put("reason", "direct_scoped_grant");
            return result;
        }

        if (hasDirect(connection, accountId, normalizedPermissionKey, null, null)) {
            result.put("allowed", true);
            result.put("reason", "direct_global_grant");
            return result;
        }

        result.put("allowed", false);
        result.put("reason", "no_matching_permission");
        return result;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}