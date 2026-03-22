package pirch.pz.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pirch.pz.db.DatabaseManager;
import pirch.pz.service.PlayerIdentity;

public final class PostgresPermissionRepository {
    private PostgresPermissionRepository() {
    }

    public static Map<String, Object> grant(
        PlayerIdentity target,
        String permissionKey,
        String scopeType,
        String scopeKey
    ) {
        requirePermissionKey(permissionKey);

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int targetAccountId = PostgresAccountRepository.resolveOrCreateAccountTransactional(connection, target);
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO permissions.account_permission "
                        + "(account_id, permission_key, scope_type, scope_key, granted_by_account_id, active) "
                        + "VALUES (?, ?, ?, ?, NULL, TRUE) "
                        + "ON CONFLICT (account_id, permission_key, scope_type, scope_key) "
                        + "DO UPDATE SET active = TRUE, updated_at = NOW()")) {
                    statement.setInt(1, targetAccountId);
                    statement.setString(2, permissionKey.trim());
                    statement.setString(3, normalize(scopeType));
                    statement.setString(4, normalize(scopeKey));
                    statement.executeUpdate();
                }

                connection.commit();
                return findGrant(connection, targetAccountId, permissionKey, scopeType, scopeKey);
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to grant permission=" + permissionKey, e);
        }
    }

    public static boolean revoke(
        PlayerIdentity target,
        String permissionKey,
        String scopeType,
        String scopeKey
    ) {
        requirePermissionKey(permissionKey);

        try (Connection connection = DatabaseManager.getConnection()) {
            int targetAccountId = PostgresAccountRepository.resolveOrCreateAccountTransactional(connection, target);
            try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE permissions.account_permission "
                    + "SET active = FALSE, updated_at = NOW() "
                    + "WHERE account_id = ? AND permission_key = ? "
                    + "AND ((scope_type IS NULL AND ? IS NULL) OR scope_type = ?) "
                    + "AND ((scope_key IS NULL AND ? IS NULL) OR scope_key = ?) "
                    + "AND active = TRUE")) {
                String normalizedScopeType = normalize(scopeType);
                String normalizedScopeKey = normalize(scopeKey);
                statement.setInt(1, targetAccountId);
                statement.setString(2, permissionKey.trim());
                statement.setString(3, normalizedScopeType);
                statement.setString(4, normalizedScopeType);
                statement.setString(5, normalizedScopeKey);
                statement.setString(6, normalizedScopeKey);
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to revoke permission=" + permissionKey, e);
        }
    }

    public static boolean has(PlayerIdentity target, String permissionKey, String scopeType, String scopeKey) {
        requirePermissionKey(permissionKey);

        try (Connection connection = DatabaseManager.getConnection()) {
            int targetAccountId = PostgresAccountRepository.resolveOrCreateAccountTransactional(connection, target);
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM permissions.account_permission "
                    + "WHERE account_id = ? AND permission_key = ? "
                    + "AND active = TRUE "
                    + "AND ((scope_type IS NULL AND ? IS NULL) OR scope_type = ?) "
                    + "AND ((scope_key IS NULL AND ? IS NULL) OR scope_key = ?)")) {
                String normalizedScopeType = normalize(scopeType);
                String normalizedScopeKey = normalize(scopeKey);
                statement.setInt(1, targetAccountId);
                statement.setString(2, permissionKey.trim());
                statement.setString(3, normalizedScopeType);
                statement.setString(4, normalizedScopeType);
                statement.setString(5, normalizedScopeKey);
                statement.setString(6, normalizedScopeKey);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check permission=" + permissionKey, e);
        }
    }

    public static List<Map<String, Object>> list(PlayerIdentity target) {
        try (Connection connection = DatabaseManager.getConnection()) {
            int targetAccountId = PostgresAccountRepository.resolveOrCreateAccountTransactional(connection, target);
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT permission_key, scope_type, scope_key, active, created_at, updated_at "
                    + "FROM permissions.account_permission "
                    + "WHERE account_id = ? AND active = TRUE "
                    + "ORDER BY permission_key ASC, scope_type ASC, scope_key ASC")) {
                statement.setInt(1, targetAccountId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<Map<String, Object>> rows = new ArrayList<>();
                    while (resultSet.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("permissionKey", resultSet.getString("permission_key"));
                        row.put("scopeType", resultSet.getString("scope_type"));
                        row.put("scopeKey", resultSet.getString("scope_key"));
                        row.put("active", resultSet.getBoolean("active"));
                        row.put("createdAt", resultSet.getTimestamp("created_at"));
                        row.put("updatedAt", resultSet.getTimestamp("updated_at"));
                        rows.add(row);
                    }
                    return rows;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list permissions", e);
        }
    }

    private static Map<String, Object> findGrant(
        Connection connection,
        int accountId,
        String permissionKey,
        String scopeType,
        String scopeKey
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT permission_key, scope_type, scope_key, active "
                + "FROM permissions.account_permission "
                + "WHERE account_id = ? AND permission_key = ? "
                + "AND ((scope_type IS NULL AND ? IS NULL) OR scope_type = ?) "
                + "AND ((scope_key IS NULL AND ? IS NULL) OR scope_key = ?)")) {
            String normalizedScopeType = normalize(scopeType);
            String normalizedScopeKey = normalize(scopeKey);
            statement.setInt(1, accountId);
            statement.setString(2, permissionKey.trim());
            statement.setString(3, normalizedScopeType);
            statement.setString(4, normalizedScopeType);
            statement.setString(5, normalizedScopeKey);
            statement.setString(6, normalizedScopeKey);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("permissionKey", resultSet.getString("permission_key"));
                row.put("scopeType", resultSet.getString("scope_type"));
                row.put("scopeKey", resultSet.getString("scope_key"));
                row.put("active", resultSet.getBoolean("active"));
                return row;
            }
        }
    }

    private static void requirePermissionKey(String permissionKey) {
        if (permissionKey == null || permissionKey.isBlank()) {
            throw new IllegalArgumentException("permissionKey is required");
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
