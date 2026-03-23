package pirch.pz.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
                "(account_id, permission_key, scope_type, scope_key, granted_by_account_id, active, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, TRUE, NOW(), NOW()) " +
                "ON CONFLICT (account_id, permission_key, COALESCE(scope_type, ''), COALESCE(scope_key, '')) DO NOTHING")) {
            statement.setInt(1, accountId);
            statement.setString(2, normalizedPermissionKey);
            statement.setString(3, normalizedScopeType);
            statement.setString(4, normalizedScopeKey);

            if (grantedByAccountId == null) {
                statement.setObject(5, null);
            } else {
                statement.setInt(5, grantedByAccountId);
            }
            try {
                statement.executeUpdate();
            } catch (SQLException ignored) {
                // fall through to update form below for databases that do not accept expression-based conflict targets here
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE permissions.account_permission " +
                "SET active = TRUE, granted_by_account_id = ?, updated_at = NOW() " +
                "WHERE account_id = ? AND permission_key = ? " +
                "AND ((scope_type IS NULL AND ? IS NULL) OR scope_type = ?) " +
                "AND ((scope_key IS NULL AND ? IS NULL) OR scope_key = ?)")) {
            if (grantedByAccountId == null) {
                statement.setObject(1, null);
            } else {
                statement.setInt(1, grantedByAccountId);
            }
            statement.setInt(2, accountId);
            statement.setString(3, normalizedPermissionKey);
            statement.setString(4, normalizedScopeType);
            statement.setString(5, normalizedScopeType);
            statement.setString(6, normalizedScopeKey);
            statement.setString(7, normalizedScopeKey);
            int updated = statement.executeUpdate();

            if (updated == 0) {
                try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO permissions.account_permission " +
                        "(account_id, permission_key, scope_type, scope_key, granted_by_account_id, active, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, TRUE, NOW(), NOW())")) {
                    insert.setInt(1, accountId);
                    insert.setString(2, normalizedPermissionKey);
                    insert.setString(3, normalizedScopeType);
                    insert.setString(4, normalizedScopeKey);
                    if (grantedByAccountId == null) {
                        insert.setObject(5, null);
                    } else {
                        insert.setInt(5, grantedByAccountId);
                    }
                    insert.executeUpdate();
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("accountId", accountId);
        result.put("permissionKey", normalizedPermissionKey);
        result.put("scopeType", normalizedScopeType);
        result.put("scopeKey", normalizedScopeKey);
        result.put("grantedByAccountId", grantedByAccountId);
        result.put("active", true);
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
            "UPDATE permissions.account_permission " +
                "SET active = FALSE, updated_at = NOW() " +
                "WHERE account_id = ? AND permission_key = ? " +
                "AND ((scope_type IS NULL AND ? IS NULL) OR scope_type = ?) " +
                "AND ((scope_key IS NULL AND ? IS NULL) OR scope_key = ?) " +
                "AND active = TRUE")) {
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
                "WHERE account_id = ? AND permission_key = ? AND active = TRUE " +
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

    public static List<Map<String, Object>> list(Connection connection, int accountId) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT account_id, permission_key, scope_type, scope_key, granted_by_account_id, active, created_at, updated_at " +
                "FROM permissions.account_permission " +
                "WHERE account_id = ? AND active = TRUE " +
                "ORDER BY permission_key ASC, scope_type ASC NULLS FIRST, scope_key ASC NULLS FIRST")) {
            statement.setInt(1, accountId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("accountId", rs.getInt("account_id"));
                    row.put("permissionKey", rs.getString("permission_key"));
                    row.put("scopeType", rs.getString("scope_type"));
                    row.put("scopeKey", rs.getString("scope_key"));

                    int grantedBy = rs.getInt("granted_by_account_id");
                    row.put("grantedByAccountId", rs.wasNull() ? null : grantedBy);

                    row.put("active", rs.getBoolean("active"));
                    row.put("createdAt", rs.getTimestamp("created_at"));
                    row.put("updatedAt", rs.getTimestamp("updated_at"));
                    results.add(row);
                }
            }
        }

        return results;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
