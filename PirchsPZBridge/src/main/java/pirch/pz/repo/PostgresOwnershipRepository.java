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

public final class PostgresOwnershipRepository {
    private PostgresOwnershipRepository() {
    }

    public static Map<String, Object> claimNode(PlayerIdentity identity, String nodeKey, String nodeType) {
        requireNodeKey(nodeKey);

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int accountId = PostgresAccountRepository.resolveOrCreateAccountTransactional(connection, identity);

                Map<String, Object> existingOwner = getNodeOwner(connection, nodeKey);
                if (existingOwner != null && !accountIdEquals(existingOwner, accountId)) {
                    throw new IllegalStateException("node is already owned by another account");
                }

                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO ownership.account_node (account_id, node_key, node_type, active) "
                        + "VALUES (?, ?, ?, TRUE) "
                        + "ON CONFLICT (node_key) DO UPDATE SET "
                        + "account_id = EXCLUDED.account_id, "
                        + "node_type = EXCLUDED.node_type, "
                        + "active = TRUE, "
                        + "updated_at = NOW()")) {
                    statement.setInt(1, accountId);
                    statement.setString(2, nodeKey);
                    statement.setString(3, normalize(nodeType, "generic"));
                    statement.executeUpdate();
                }

                connection.commit();
                return getNodeOwner(connection, nodeKey);
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to claim node=" + nodeKey, e);
        }
    }

    public static boolean releaseNode(PlayerIdentity identity, String nodeKey) {
        requireNodeKey(nodeKey);

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int accountId = PostgresAccountRepository.resolveOrCreateAccountTransactional(connection, identity);

                try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE ownership.account_node SET active = FALSE, updated_at = NOW() "
                        + "WHERE node_key = ? AND account_id = ? AND active = TRUE")) {
                    statement.setString(1, nodeKey);
                    statement.setInt(2, accountId);
                    boolean changed = statement.executeUpdate() > 0;
                    connection.commit();
                    return changed;
                }
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to release node=" + nodeKey, e);
        }
    }

    public static Map<String, Object> getNodeOwner(String nodeKey) {
        requireNodeKey(nodeKey);
        try (Connection connection = DatabaseManager.getConnection()) {
            return getNodeOwner(connection, nodeKey);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load node owner for node=" + nodeKey, e);
        }
    }

    public static List<Map<String, Object>> listOwnedNodes(PlayerIdentity identity) {
        try (Connection connection = DatabaseManager.getConnection()) {
            int accountId = PostgresAccountRepository.resolveOrCreateAccountTransactional(connection, identity);
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT node_key, node_type, active, created_at, updated_at "
                    + "FROM ownership.account_node "
                    + "WHERE account_id = ? AND active = TRUE "
                    + "ORDER BY node_key ASC")) {
                statement.setInt(1, accountId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<Map<String, Object>> rows = new ArrayList<>();
                    while (resultSet.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("nodeKey", resultSet.getString("node_key"));
                        row.put("nodeType", resultSet.getString("node_type"));
                        row.put("active", resultSet.getBoolean("active"));
                        row.put("createdAt", resultSet.getTimestamp("created_at"));
                        row.put("updatedAt", resultSet.getTimestamp("updated_at"));
                        rows.add(row);
                    }
                    return rows;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list owned nodes", e);
        }
    }

    private static Map<String, Object> getNodeOwner(Connection connection, String nodeKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT n.node_key, n.node_type, n.account_id, a.canonical_external_id, a.account_name "
                + "FROM ownership.account_node n "
                + "JOIN accounts.account a ON a.account_id = n.account_id "
                + "WHERE n.node_key = ? AND n.active = TRUE")) {
            statement.setString(1, nodeKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("nodeKey", resultSet.getString("node_key"));
                row.put("nodeType", resultSet.getString("node_type"));
                row.put("accountId", resultSet.getInt("account_id"));
                row.put("accountExternalId", resultSet.getString("canonical_external_id"));
                row.put("accountName", resultSet.getString("account_name"));
                return row;
            }
        }
    }

    private static void requireNodeKey(String nodeKey) {
        if (nodeKey == null || nodeKey.isBlank()) {
            throw new IllegalArgumentException("nodeKey is required");
        }
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static boolean accountIdEquals(Map<String, Object> data, int accountId) {
        Object value = data.get("accountId");
        if (value instanceof Number number) {
            return number.intValue() == accountId;
        }
        return false;
    }
}
